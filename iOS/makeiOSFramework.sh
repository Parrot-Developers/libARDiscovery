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
		xcodebuild -project $LIBNAME.xcodeproj -configuration Release -sdk iphoneos -arch armv7 || exit 1
		xcodebuild -project $LIBNAME.xcodeproj -configuration Release -sdk iphoneos -arch armv7s || exit 1
		xcodebuild -project $LIBNAME.xcodeproj -configuration Release -sdk iphonesimulator || exit 1
		ARCHS=$(ls Products)
		LIBS=""
		for ARCH in $ARCHS; do
			cp -r Products/$ARCH/lib$LIBNAME.framework $FRAMEWORK_PATH
			AFILE=$(ls Products/$ARCH/lib$LIBNAME.framework/lib*)
			LIBS=$LIBS" "$AFILE
		done
		FWLIB=$(echo $AFILE | sed 's:.*/lib\(.*\):lib\1:')
		lipo $LIBS -create -output $FRAMEWORK_PATH/lib$LIBNAME.framework/$FWLIB
		;;
	debug)
		xcodebuild -project $LIBNAME.xcodeproj -configuration Debug -sdk iphoneos -arch armv7 || exit 1
		xcodebuild -project $LIBNAME.xcodeproj -configuration Debug -sdk iphoneos -arch armv7s || exit 1
		xcodebuild -project $LIBNAME.xcodeproj -configuration Debug -sdk iphonesimulator || exit 1
		ARCHS=$(ls Products)
		LIBS=""
		for ARCH in $ARCHS; do
			cp -r Products/$ARCH/lib$LIBNAME"_dbg.framework" $FRAMEWORK_PATH
			AFILE=$(ls Products/$ARCH/lib$LIBNAME"_dbg.framework"/lib*)
			LIBS=$LIBS" "$AFILE
		done
		FWLIB=$(echo $AFILE | sed 's:.*/lib\(.*\):lib\1:')
		lipo $LIBS -create -output $FRAMEWORK_PATH/lib$LIBNAME"_dbg.framework"/$FWLIB
		;;
	clea)n
		xcodebuild -project $LIBNAME.xcodeproj -configuration Release -sdk iphoneos clean
		xcodebuild -project $LIBNAME.xcodeproj -configuration Release -sdk iphonesimulator clean
		xcodebuild -project $LIBNAME.xcodeproj -configuration Debug -sdk iphoneos clean
		xcodebuild -project $LIBNAME.xcodeproj -configuration Debug -sdk iphonesimulator clean
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
