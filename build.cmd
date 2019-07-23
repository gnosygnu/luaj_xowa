REM NOTE: must set encoding else "unmappable character for encoding utf-8"; https://stackoverflow.com/questions/21147974/set-a-system-property-with-ant
SET ANT_OPTS="-Dfile.encoding=UTF8"
ant -buildfile build.xml