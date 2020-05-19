#! /bin/bash
if [ "$#" -ne 2 ]; then
    echo -e "Illegal number of parameters! should be \n$0 src.osm dst.map"
    echo -e "but was $*"
    exit 1
fi
SOURCE=$1
DEST=$2
if [ ! -f "$SOURCE" ]; then
    echo "$SOURCE does not exist"
    exit 2
fi
if [ -f "$DEST" ]; then
    echo "$DEST already exists. not overwriting it"
    exit 3
fi

/opt/osmosis/bin/osmosis --rx file=$SOURCE --mw file=$DEST