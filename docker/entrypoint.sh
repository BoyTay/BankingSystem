#!/bin/sh
set -eu

mode="${1:-gui}"
classpath='/app/classes:/app/lib/*'

exec 9>/data/.vietbank.lock
if ! flock -n 9; then
    echo 'Another VietBank container is using this data volume. Stop it before switching GUI/CLI.' >&2
    exit 1
fi

if [ "$mode" = 'cli' ]; then
    exec java -cp "$classpath" com.banking.Main
fi

if [ "$mode" != 'gui' ]; then
    echo "Unknown mode: $mode (use gui or cli)" >&2
    exit 2
fi

export DISPLAY=:99
vnc_width="${VNC_WIDTH:-1280}"
vnc_height="${VNC_HEIGHT:-800}"
Xvfb :99 -screen 0 "${vnc_width}x${vnc_height}x24" -nolisten tcp &
xvfb_pid=$!
for _ in 1 2 3 4 5 6 7 8 9 10; do
    [ -S /tmp/.X11-unix/X99 ] && break
    sleep 0.2
done
if [ ! -S /tmp/.X11-unix/X99 ]; then
    echo 'Xvfb did not start' >&2
    exit 1
fi
x11vnc -display :99 -rfbport 5900 -localhost -forever -shared -nopw -quiet &
vnc_pid=$!
websockify --web=/usr/share/novnc 6080 localhost:5900 &
web_pid=$!

cleanup() {
    kill "$web_pid" "$vnc_pid" "$xvfb_pid" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

java -cp "$classpath" com.banking.ui.GuiLauncher
