"use strict";

var exec = require("cordova/exec");

var bTOBDSpeedPlugin = {
	start: function(sc, ec) {
		exec(sc, ec, "BTOBDSpeedPlugin", "start", []);
	}
};

module.exports = bTOBDSpeedPlugin;