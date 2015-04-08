String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

function initialize_zurb() {
	Ember.$(document).foundation();
}

function locate(obj, path) {
	if (!path) return null;
	if (path[0] == '{' && path[path.length-1] == '}') {
		// strip leading/trailing curlies, if present
		path = path.substring(1, path.length-1);
	}
	path = path.split('.');
	var arrayPattern = /(.+)\[(\d+)\]/;
	for (var i = 0; i < path.length; i++) {
		var match = arrayPattern.exec(path[i]);
		if (match) {
			obj = obj[match[1]][parseInt(match[2])];
		} else {
			obj = obj[path[i]];
		}
	}

	return obj;
}

function getParameterByName(name) {
	name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
	var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
		results = regex.exec(location.search);
	return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}


function timestampToString(time) {
	var convertedDate = new Date(0);
	convertedDate.setUTCSeconds(Math.round(time/1000));
	return convertedDate.toLocaleString();
}
