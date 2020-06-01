#!/usr/bin/env bash
#
# 	Use case: Installs osmosis, mapsforge-mapfile-writer plugin
# 	Author:   Tom Morelly
# 	Date:     2019.06.08
#
##########################################################################

set -e
OSMOSIS_DOWNLOAD_URL="https://bretth.dev.openstreetmap.org/osmosis-build/osmosis-0.46.tgz"
MAP_WRITER_DOWNLOAD_URL="https://search.maven.org/remotecontent?filepath=org/mapsforge/mapsforge-map-writer/0.9.0/mapsforge-map-writer-0.9.0-jar-with-dependencies.jar"
OUTPUT_PATH="/opt"
GREEN='\033[0;32m'
NC='\033[0m'

function install_osmosis(){
	# download and build osmosis
	cd $OUTPUT_PATH
	wget $OSMOSIS_DOWNLOAD_URL
	mkdir -p osmosis
	mv osmosis-0.46.tgz osmosis
	cd osmosis
	tar xvfz osmosis-0.46.tgz
	rm osmosis-0.46.tgz
	chmod a+x bin/osmosis
}

function install_mapwriter_plugin(){
	# download and imports mapwriter plugin
	cd /opt/osmosis/lib/default
	curl -L -O $MAP_WRITER_DOWNLOAD_URL
}

function osmosis_usage(){
	echo -e "
	\nosmosis usage:
	/opt/osmosis/bin/osmosis --rx file=path-to-osm-file.osm --mw file=destination-path-map-file.map
	"
}

function main(){
	if [[ $UID -ne 0 ]];then
        	echo -ne "Error: needs to run with sudo.\nExiting.\n";
	        exit 1;
	fi

	echo -e "\n${GREEN}Downloading osmosis to $OUTPUT_PATH/osmosis.${NC}"
	install_osmosis;
    	echo -e "\n${GREEN}Downloading mapwriter plugin $OUTPUT_PATH/osmosis.${NC}"
	install_mapwriter_plugin;
	echo -e "\n${GREEN}Download and build successful finished.${NC}"
	osmosis_usage
	exit 0;
}

main
