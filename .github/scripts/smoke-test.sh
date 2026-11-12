#!/usr/bin/env bash
# Smoke test of the plugin zip in a real Open Integration Engine: unpacks the engine release,
# installs the zip, starts the server, deploys the channel of .github/smoke/SmokeChannel.java
# (HTTP Listener, data type EDIFACT in and XML out, a destination back to EDIFACT) and posts two
# interchanges:
#
#   examples/orders.edi   200, stored as EDIFACT XML (release characters undone), and the
#                         destination wrote it back as EDIFACT (release characters again)
#   examples/medlab.edi   200, a Dutch MEDLAB message stored as EDIFACT XML
#
# This covers what the unit tests cannot: the engine loads the extension (exact mirthVersion),
# the data type works in both directions in a deployed channel, and the channel XML of this
# plugin version imports into the engine.
#
# Usage: smoke-test.sh <engine version, e.g. 4.7.0> <plugin zip>
# Environment: JAVA (default java; must be a JDK, the channel generator runs as a source file),
# OIE_DOWNLOADS (default ~/.cache/oie), SMOKE_DIR (default a temporary folder, kept on failure),
# SMOKE_HTTP_PORT / SMOKE_HTTPS_PORT (default 8080 / 8443), SMOKE_LISTENER_PORT (default 8090).
set -euo pipefail

version="${1:?engine version, e.g. 4.7.0}"
zip="$(cd "$(dirname "${2:?plugin zip}")" && pwd)/$(basename "$2")"
repo="$(cd "$(dirname "$0")/../.." && pwd)"
java="${JAVA:-java}"
http_port="${SMOKE_HTTP_PORT:-8080}"
https_port="${SMOKE_HTTPS_PORT:-8443}"
listener_port="${SMOKE_LISTENER_PORT:-8090}"
password="smoke-test-admin-password"
channel_id="5e0b7e5f-f41c-4d1e-9a6c-000000000003"
work="${SMOKE_DIR:-$(mktemp -d)}"
oie="${work}/oie"
api="https://localhost:${https_port}/api"
jar="${work}/cookies.txt"
sep=":"
native() { echo "$1"; }
case "$(uname -s)" in MINGW* | MSYS* | CYGWIN*) sep=";"; native() { cygpath -m "$1"; } ;; esac

server_pid=""
stop_server() {
  if [ -n "${server_pid}" ] && kill -0 "${server_pid}" 2>/dev/null; then
    echo "Stopping the engine"
    kill "${server_pid}" 2>/dev/null || true
    for _ in $(seq 1 30); do kill -0 "${server_pid}" 2>/dev/null || break; sleep 1; done
    kill -9 "${server_pid}" 2>/dev/null || true
  fi
}
fail() {
  echo "SMOKE TEST FAILED: $*" >&2
  echo "--- last lines of the server log (${oie}/logs/mirth.log)" >&2
  tail -n 60 "${oie}/logs/mirth.log" 2>/dev/null >&2 || tail -n 60 "${work}/server.out" >&2 || true
  exit 1
}
trap stop_server EXIT

# ------------------------------------------------------------------ engine with the plugin

tarball="$(bash "${repo}/.github/scripts/download-oie.sh" "${version}" "${OIE_DOWNLOADS:-${HOME}/.cache/oie}")"
echo "Unpacking ${tarball} in ${work}"
rm -rf "${oie}"
mkdir -p "${work}"
tar -xzf "${tarball}" -C "${work}"
rm -rf "${oie}/extensions/datatype-edifact"
unzip -q "${zip}" -d "${oie}/extensions"
plugin_version="$(sed -n 's|.*<pluginVersion>\(.*\)</pluginVersion>.*|\1|p' "${oie}/extensions/datatype-edifact/plugin.xml")"
mirth_versions="$(sed -n 's|.*<mirthVersion>\(.*\)</mirthVersion>.*|\1|p' "${oie}/extensions/datatype-edifact/plugin.xml")"
echo "Plugin ${plugin_version}, mirthVersion ${mirth_versions}, engine ${version}"
case ",${mirth_versions}," in *",${version},"*) ;; *) fail "plugin.xml mirthVersion (${mirth_versions}) does not list ${version}: the engine would not load it" ;; esac

props="${oie}/conf/mirth.properties"
sed -i.bak -e "s|^http.port *=.*|http.port = ${http_port}|" -e "s|^https.port *=.*|https.port = ${https_port}|" -e '/^server.initialadminpassword/d' "${props}"
echo "server.initialadminpassword = ${password}" >> "${props}"

# The options of the engine's own launcher (oieserver.vmoptions), heap included.
args="${work}/java-options.txt"
: > "${args}"
# custom.vmoptions may hold only comments, and then grep finds nothing (exit 1).
for f in conf/base_includes.vmoptions conf/default_modules.vmoptions conf/custom.vmoptions; do
  if [ -f "${oie}/${f}" ]; then
    grep -v -e '^\s*#' -e '^\s*$' -e '^-include-options' "${oie}/${f}" | tr -d '\r' >> "${args}" || true
  fi
done

echo "Starting the engine ($("${java}" -version 2>&1 | head -1))"
(cd "${oie}" && exec "${java}" "@$(native "${args}")" -jar mirth-server-launcher.jar > "${work}/server.out" 2>&1) &
server_pid=$!

curl_api() { curl -sk -b "${jar}" -c "${jar}" -H "X-Requested-With: smoke-test" "$@"; }

echo "Waiting for the engine to accept a login"
logged_in=""
for _ in $(seq 1 120); do
  kill -0 "${server_pid}" 2>/dev/null || fail "the engine stopped while starting"
  for pw in "${password}" admin; do
    code="$(curl_api -o /dev/null -w '%{http_code}' -X POST "${api}/users/_login" --data-urlencode username=admin --data-urlencode "password=${pw}" || true)"
    if [ "${code}" = "200" ]; then logged_in=1; break 2; fi
  done
  sleep 3
done
[ -n "${logged_in}" ] || fail "no login within 6 minutes"
echo "Logged in"

code="$(curl_api -o "${work}/plugins.xml" -w '%{http_code}' -H "Accept: application/xml" "${api}/extensions/plugins" || true)"
grep -q "<name>EDIFACT Data Type</name>" "${work}/plugins.xml" \
  || { echo "HTTP ${code}" >&2; head -c 2000 "${work}/plugins.xml" >&2; echo >&2; fail "the engine did not load the EDIFACT Data Type extension"; }
echo "Extension loaded"

# ------------------------------------------------------------------ channel

o="$(native "${oie}")"
# The engine jars first: they contain their own public copies of Rhino classes such as NativeDate,
# which must win over server-lib/rhino-*.jar. A wildcard lists jars in directory order, which
# differs between Windows and Linux.
cp="${o}/server-lib/mirth-client-core.jar${sep}${o}/server-lib/mirth-server.jar${sep}${o}/server-lib/*${sep}${o}/server-lib/donkey/*${sep}${o}/server-lib/commons/*${sep}${o}/server-lib/log4j/*${sep}${o}/extensions/http/*${sep}${o}/extensions/vm/*${sep}${o}/extensions/datatype-xml/*${sep}${o}/extensions/datatype-edifact/datatype-edifact-shared.jar"
"${java}" -cp "${cp}" "$(native "${repo}/.github/smoke/SmokeChannel.java")" "${version}" "${listener_port}" "$(native "${work}/channel.xml")" > "${work}/generator.out" 2>&1 \
  || { cat "${work}/generator.out" >&2; fail "could not generate the channel"; }

code="$(curl_api -o "${work}/import.out" -w '%{http_code}' -X POST "${api}/channels" -H "Content-Type: application/xml" --data-binary "@${work}/channel.xml")"
[ "${code}" = "200" ] || { cat "${work}/import.out" >&2; fail "channel import returned HTTP ${code}"; }
code="$(curl_api -o "${work}/deploy.out" -w '%{http_code}' -X POST "${api}/channels/${channel_id}/_deploy")"
[ "${code}" = "204" ] || [ "${code}" = "200" ] || { cat "${work}/deploy.out" >&2; fail "deploy returned HTTP ${code}"; }

started=""
for _ in $(seq 1 60); do
  if curl_api -H "Accept: application/xml" "${api}/channels/${channel_id}/status" | grep -q "<state>STARTED</state>"; then started=1; break; fi
  sleep 2
done
[ -n "${started}" ] || fail "the channel did not start"
echo "Channel deployed"

# ------------------------------------------------------------------ messages

# The listener answers 200 without a body, so the messages are read back through the engine's
# API (messages.xml, with the XML and the EDIFACT of each message escaped in it).
post() { # <name> <file>
  local status
  status="$(curl -s -o "${work}/$1.response" -w '%{http_code}' -X POST "http://localhost:${listener_port}/" \
    -H "Content-Type: text/plain; charset=ISO-8859-1" --data-binary "@$2" --max-time 120)"
  if [ "${status}" != "200" ]; then
    echo "--- response" >&2; cat "${work}/$1.response" >&2; echo >&2
    fail "$1: expected HTTP 200, got HTTP ${status}"
  fi
  echo "OK  $1: HTTP ${status}"
}

messages() { # the channel's messages with their content, unescaped, in ${work}/messages.xml
  curl_api -H "Accept: application/xml" "${api}/channels/${channel_id}/messages?limit=20&includeContent=true" \
    | sed -e 's/&lt;/</g' -e 's/&gt;/>/g' -e 's/&quot;/"/g' -e "s/&apos;/'/g" -e "s/&#39;/'/g" -e 's/&amp;/\&/g' > "${work}/messages.xml"
}

encoded_edifact() { # the content the data type wrote as EDIFACT (the destination's encoded content)
  awk '/<contentType>ENCODED<\/contentType>/ { e = 1; next }
       e && /<content>/ { c = 1; buf = "" }
       c { buf = buf $0 "\n" }
       c && /<\/content>/ { c = 0; e = 0; getline dt; if (dt ~ /<dataType>EDIFACT</) printf "%s", buf }' "${work}/messages.xml" > "${work}/encoded.edi"
}

expect_content() { # <text> <what>
  grep -qF "$1" "${work}/messages.xml" || fail "$2: '$1' is not in the messages (see ${work}/messages.xml)"
  echo "OK  $2"
}

received() { # the number of messages the channel received
  curl_api -H "Accept: application/xml" "${api}/channels/${channel_id}/statistics" | sed -n 's|.*<received>\([0-9]*\)</received>.*|\1|p' | head -1
}

expect_received() { # <expected count> <what>
  local n=""
  for _ in $(seq 1 30); do
    n="$(received)"
    [ "${n}" = "$1" ] && { echo "OK  $2: ${n} messages received"; return; }
    sleep 1
  done
  fail "$2: expected $1 messages received, the channel reports '${n}'"
}

post orders "${repo}/examples/orders.edi"
expect_received 1 "orders"
messages
expect_content '<UNH.02.1>ORDERS</UNH.02.1>' "orders: message type in the XML"
expect_content '<NAD.04.1>Acme + Co</NAD.04.1>' "orders: release character undone in the XML"
encoded_edifact
grep -qF "NAD+SU+++Acme ?+ Co+Main Street 1" "${work}/encoded.edi"   || fail "orders: the destination did not write EDIFACT with the release character (see ${work}/encoded.edi)"
echo "OK  orders: XML back to EDIFACT, release character again"

post medlab "${repo}/examples/medlab.edi"
expect_received 2 "medlab"
messages
expect_content '<UNH.02.1>MEDLAB</UNH.02.1>' "medlab: message type in the XML"
if grep -q "<status>ERROR</status>" "${work}/messages.xml"; then fail "a message ended in ERROR (see ${work}/messages.xml)"; fi

if grep -E "NoClassDefFoundError|ClassNotFoundException|EDIFACT.*Exception" "${oie}/logs/mirth.log" "${work}/server.out" 2>/dev/null; then
  fail "the server log shows a class loading or EDIFACT problem"
fi

echo "Smoke test passed: plugin ${plugin_version} on OIE ${version}"
