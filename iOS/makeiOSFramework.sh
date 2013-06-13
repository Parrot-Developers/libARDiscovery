#!/bin/sh

CURDIR=$(pwd)
LIBNAME="ARDiscovery"

# $1 will always be the --prefix=... arg
# we assume that the prefix is ALSO the install dir of other libs

# $2 is the action (release, debug, clean)

PREFIX=$1
FRAMEWORK_PATH=$PREFIX/Frameworks/
CONFIGURATION=$(echo $2 | tr [:upper:] [:lower:])

echo "Building "$LIBNAME" with prefix : <"$PREFIX">"
echo "And target : <"$CONFIGURATION">"

# If any arg is missing, return
if [ -z $PREFIX ] || [ -z $CONFIGURATION ]; then
	echo "Missing args !"
	echo " usage:"
	echo $0 "install/dir action"
	echo ""
	echo "Valid actions:"
	echo " - release > Build the lib in release mode"
	echo " - debug   > Build the lib in debug mode"
	echo " - clean   > Remove all debug/release products"
	exit 1
fi

# run xcodebuild with good arg
case $CONFIGURATION in
	release)
		xcodebuild -project $LIBNAME/$LIBNAME.xcodeproj -configuration Release || exit 1
		cp -r $LIBNAME/Products/lib$LIBNAME.framework $PREFIX/Frameworks/
		;;
	debug)
		xcodebuild -project $LIBNAME/$LIBNAME.xcodeproj -configuration Debug || exit 1
		cp -r $LIBNAME/Products/lib$LIBNAME"_dbg.framework" $PREFIX/Frameworks/
		;;
	clean)
		xcodebuild -project $LIBNAME/$LIBNAME.xcodeproj -configuration Release clean
		xcodebuild -project $LIBNAME/$LIBNAME.xcodeproj -configuration Debug clean
		rm -r $FRAMEWORK_PATH/$LIBNAME'.framework' 2>/dev/null
		rm -r $FRAMEWORK_PATH/$LIBNAME'_dbg.framework' 2>/dev/null
		;;
	*)
		echo "Unknown action <$CONFIGURATION>"
		echo "Valid actions are: release, debug, clean"
		exit 1
		;;
esac

exit 0
