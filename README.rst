Supported Platform
==================

Android Only

Installation
============

``cordova plugin add https://github.com/six519/cordova-plugin-btobdspeed.git``

Usage
=====
::

    window.bTOBDSpeedPlugin.start(function(e){
        console.log('The speed is: ' + e + 'km/h');
    }, function(e){
        alert('Error: ' + e);
    });