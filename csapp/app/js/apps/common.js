String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

function setCookie(cname, cvalue, exdays){
	var d = new Date();
	d.setTime(d.getTime() + (exdays*24*60*60*1000));
	var expires="expires="+d.toUTCString();
	document.cookie = cname + "=" + cvalue + "; " + expires;
}

function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');
	for(var i=0; i<ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0)===' ') {
			c = c.substring(1);
		}
		if (c.indexOf(name) !== -1) {
			return c.substring(name.length, c.length);
		}
	}
	return "";
}

function checkCookie(cname) {
	var cookie = getCookie(cname);
	if (cookie !== "") {
		return true;
	} else {
		return false;
	}
}


function generateDeviceId()
{
	var text = "";
	var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	for( var i=0; i < 20; i++ ){
		text += possible.charAt(Math.floor(Math.random() * possible.length));
	}
	return text;
}

function getDeviceName(){
	return navigator.userAgent;
}


CloudOs = {
	json_safe_parse: function (j) {
		return j ? JSON.parse(j) : null;
	},

	login: function (auth_response) {
		sessionStorage.setItem('cloudos_session', auth_response.sessionId);
		CloudOs.set_account(auth_response.account);
	},

	logout: function () {
		sessionStorage.clear();
	},

	account: function () {
		var cs_acct = CloudOs.json_safe_parse(sessionStorage.getItem('cloudos_account'));
		if (!Ember.isNone(cs_acct)){
			cs_acct.availableApps = cs_acct.availableApps.filter(function(app) {
				return app.interactive === true;
			});
		}
		// cs_acct = add_icon_data(cs_acct);
		return cs_acct;
	},

	set_account: function (account) {
		sessionStorage.setItem('cloudos_account', JSON.stringify(account));
	},

	get_app: function(app_name) {
		var cs_acct = CloudOs.json_safe_parse(sessionStorage.getItem('cloudos_account'));
		return cs_acct.availableApps.findBy('name', app_name);
	}

};

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

function add_icon_data(acct){
	var curr_acct = acct;
	if (curr_acct){
		var arrayLength = curr_acct.availableApps.length;
		for (var i = 0; i < arrayLength; i++) {
			if (curr_acct.availableApps[i].name == 'roundcube'){
				curr_acct.availableApps[i].icon_name = 'icon-envelope';
			}

			if (curr_acct.availableApps[i].name == 'roundcube-calendar'){
				curr_acct.availableApps[i].icon_name = 'icon-calendar';
			}

			if (curr_acct.availableApps[i].name == 'owncloud'){
				curr_acct.availableApps[i].icon_name = 'icon-folder';
			}

			if (curr_acct.availableApps[i].name == 'kanban'){
				curr_acct.availableApps[i].icon_name = 'icon-tasks';
			}

		}
	}
	return curr_acct;
}

function getParameterByName(name) {
	name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
	var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
		results = regex.exec(location.search);
	return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}

// Temporary TZ list, will be delivered via API in the future

var timeZoneList = [{ id:0,  dfault:false, ioffset:-720, offset:"GMT-12:00", dname:"Etc/GMT+12", lname:"International Date Line West"},
					{ id:1,  dfault:false, ioffset:-660, offset:"GMT-11:00", dname:"Pacific/Samoa", lname:"Midway Island, Samoa"},
					{ id:2,  dfault:false, ioffset:-600, offset:"GMT-10:00", dname:"HST", lname:"Hawaii"},
					{ id:3,  dfault:false, ioffset:-540, offset:"GMT-9:00", dname:"AST", lname:"Alaska"},
					{ id:4,  dfault:false, ioffset:-480, offset:"GMT-8:00", dname:"PST", lname:"Pacific Time (US & Canada)"},
					{ id:5,  dfault:false, ioffset:-420, offset:"GMT-7:00", dname:"MST", lname:"Mountain Time (US & Canada)"},
					{ id:6,  dfault:false, ioffset:-420, offset:"GMT-7:00", dname:"MST", lname:"Chihuahua, La Paz, Mazatlan"},
					{ id:7,  dfault:false, ioffset:-420, offset:"GMT-7:00", dname:"US/Arizona", lname:"Arizona"},
					{ id:8,  dfault:false, ioffset:-360, offset:"GMT-6:00", dname:"CST", lname:"Central Time (US & Canada)"},
					{ id:9,  dfault:false, ioffset:-360, offset:"GMT-6:00", dname:"CST", lname:"Guadalajara, Mexico City, Monterrey"},
					{ id:10, dfault:false, ioffset:-360, offset:"GMT-6:00", dname:"Canada/Saskatchewan", lname:"Saskatchewan"},
					{ id:11, dfault:false, ioffset:-300, offset:"GMT-5:00", dname:"EST", lname:"Eastern Time (US & Canada)"},
					{ id:12, dfault:false, ioffset:-300, offset:"GMT-5:00", dname:"America/Indianapolis", lname:"Indiana (East)"},
					{ id:13, dfault:false, ioffset:-300, offset:"GMT-5:00", dname:"America/Bogota", lname:"Bogota, Lima, Quito"},
					{ id:14, dfault:false, ioffset:-240, offset:"GMT-4:00", dname:"Canada/Atlantic", lname:"Atlantic Time (Canada)"},
					{ id:15, dfault:false, ioffset:-240, offset:"GMT-4:00", dname:"America/Caracas", lname:"Caracas, La Paz"},
					{ id:16, dfault:false, ioffset:-240, offset:"GMT-4:00", dname:"America/Santiago", lname:"Santiago"},
					{ id:17, dfault:false, ioffset:-210, offset:"GMT-3:30", dname:"Canada/Newfoundland", lname:"Newfoundland"},
					{ id:18, dfault:false, ioffset:-180, offset:"GMT-3:00", dname:"BET", lname:"Brasilia"},
					{ id:19, dfault:false, ioffset:-180, offset:"GMT-3:00", dname:"America/Buenos_Aires", lname:"Buenos Aires, Georgetown"},
					{ id:20, dfault:false, ioffset:-180, offset:"GMT-3:00", dname:"America/Godthab", lname:"Greenland"},
					{ id:21, dfault:false, ioffset:-120, offset:"GMT-2:00", dname:"Etc/GMT+2", lname:"Mid-Altantic"},
					{ id:22, dfault:false, ioffset:-60, offset:"GMT-1:00", dname:"Atlantic/Azores", lname:"Azores"},
					{ id:23, dfault:false, ioffset:-60, offset:"GMT-1:00", dname:"Atlantic/Cape_Verde", lname:"Cape Verde"},
					{ id:24, dfault:false, ioffset:0, offset:"GMT", dname:"Africa/Casablanca", lname:"Casablanca, Monrovia"},
					{ id:25, dfault:false, ioffset:0, offset:"GMT", dname:"GB", lname:"GMT: London, Dublin, Lisbon"},
					{ id:26, dfault:false, ioffset:60, offset:"GMT+1:00", dname:"CET", lname:"Central European Time"},
					{ id:27, dfault:false, ioffset:120, offset:"GMT+2:00", dname:"EET", lname:"Eastern European Time"},
					{ id:28, dfault:false, ioffset:120, offset:"GMT+2:00", dname:"CAT", lname:"Harare, Pretoria"},
					{ id:29, dfault:false, ioffset:120, offset:"GMT+2:00", dname:"Israel", lname:"Israel Standard Time"},
					{ id:30, dfault:false, ioffset:180, offset:"GMT+3:00", dname:"Asia/Baghdad", lname:"Baghdad"},
					{ id:31, dfault:false, ioffset:180, offset:"GMT+3:00", dname:"Asia/Kuwait", lname:"Arabia Standard Time"},
					{ id:32, dfault:false, ioffset:180, offset:"GMT+3:00", dname:"Europe/Moscow", lname:"Moscow"},
					{ id:33, dfault:false, ioffset:180, offset:"GMT+3:00", dname:"EAT", lname:"Eastern African Time"},
					{ id:34, dfault:false, ioffset:210, offset:"GMT+3:30", dname:"Iran", lname:"Iran"},
					{ id:35, dfault:false, ioffset:240, offset:"GMT+4:00", dname:"Asia/Dubai", lname:"Dubai, Muscat"},
					{ id:36, dfault:false, ioffset:240, offset:"GMT+4:00", dname:"Asia/Baku", lname:"Baku, Tbilisi, Yerevan"},
					{ id:37, dfault:false, ioffset:270, offset:"GMT+4:30", dname:"Asia/Kabul", lname:"Afganistan"},
					{ id:38, dfault:false, ioffset:300, offset:"GMT+5:00", dname:"Asia/Yekaterinburg", lname:"Ekaterinburg"},
					{ id:39, dfault:false, ioffset:300, offset:"GMT+5:00", dname:"Asia/Karachi", lname:"Islamabad, Karachi, Tashkent"},
					{ id:40, dfault:false, ioffset:330, offset:"GMT+5:30", dname:"IST", lname:"India Standard Time"},
					{ id:41, dfault:false, ioffset:345, offset:"GMT+5:45", dname:"Asia/Katmandu", lname:"Nepal"},
					{ id:42, dfault:false, ioffset:360, offset:"GMT+6:00", dname:"Asia/Almaty", lname:"Almaty, Novosibirsk"},
					{ id:43, dfault:false, ioffset:360, offset:"GMT+6:00", dname:"Asia/Dhaka", lname:"Astana, Dhaka"},
					{ id:44, dfault:false, ioffset:360, offset:"GMT+6:00", dname:"Asia/Colombo", lname:"Sri Lanka"},
					{ id:45, dfault:false, ioffset:390, offset:"GMT+6:30", dname:"Asia/Rangoon", lname:"Rangoon"},
					{ id:46, dfault:false, ioffset:420, offset:"GMT+7:00", dname:"Asia/Bangkok", lname:"Bangkok, Hanoi, Jakarta"},
					{ id:47, dfault:false, ioffset:420, offset:"GMT+7:00", dname:"Asia/Krasnoyarsk", lname:"Krasnoyarsk"},
					{ id:48, dfault:false, ioffset:480, offset:"GMT+8:00", dname:"Asia/Hong_Kong", lname:"Beijing, Hong Kong, Chongquing"},
					{ id:49, dfault:false, ioffset:480, offset:"GMT+8:00", dname:"Asia/Irkutsk", lname:"Irkutsk, Ulaan Bataar"},
					{ id:50, dfault:false, ioffset:480, offset:"GMT+8:00", dname:"Asia/Kuala_Lumpur", lname:"Kuala Lumpur, Singapore"},
					{ id:51, dfault:false, ioffset:480, offset:"GMT+8:00", dname:"Australia/Perth", lname:"Perth"},
					{ id:52, dfault:false, ioffset:480, offset:"GMT+8:00", dname:"Asia/Taipei", lname:"Taipei"},
					{ id:53, dfault:false, ioffset:540, offset:"GMT+9:00", dname:"JST", lname:"Tokyo, Osaka, Sapporo"},
					{ id:54, dfault:false, ioffset:540, offset:"GMT+9:00", dname:"Asia/Yakutsk", lname:"Yakutsk"},
					{ id:55, dfault:false, ioffset:570, offset:"GMT+9:30", dname:"Australia/Adelaide", lname:"Adelaide"},
					{ id:56, dfault:false, ioffset:570, offset:"GMT+9:30", dname:"Australia/Darwin", lname:"Darwin"},
					{ id:57, dfault:false, ioffset:600, offset:"GMT+10:00", dname:"Australia/Brisbane", lname:"Brisbane"},
					{ id:58, dfault:false, ioffset:600, offset:"GMT+10:00", dname:"Australia/Canberra", lname:"Canberra, Melbourne, Sydney"},
					{ id:59, dfault:false, ioffset:600, offset:"GMT+10:00", dname:"Pacific/Guam", lname:"Guam, Port Moresby"},
					{ id:60, dfault:false, ioffset:600, offset:"GMT+10:00", dname:"Australia/Hobart", lname:"Tasmania, Hobart"},
					{ id:61, dfault:false, ioffset:600, offset:"GMT+10:00", dname:"Asia/Vladivostok", lname:"Vladivostok"},
					{ id:62, dfault:false, ioffset:660, offset:"GMT+11:00", dname:"Asia/Magadan", lname:"Magadan, New Caledonia"},
					{ id:63, dfault:false, ioffset:720, offset:"GMT+12:00", dname:"Pacific/Auckland", lname:"Auckland, Wellington"},
					{ id:64, dfault:false, ioffset:720, offset:"GMT+12:00", dname:"Asia/Kamchatka", lname:"Kamchatka"},
					{ id:65, dfault:false, ioffset:720, offset:"GMT+12:00", dname:"Pacific/Fiji", lname:"Fiji"}];

Validator = {
	validateTwoFactorVerificationCode: function(code){
		var errors = {"verificationCode": null};
		var error_msg = locate(Em.I18n.translations, 'errors');
		var codeRegexp = /^(\d){7}$/;

		if ((!code) || (code.trim() == '')){
			errors.verificationCode = error_msg.field_required;
		}
		else if (!codeRegexp.test(code)){
			errors.verificationCode = error_msg.two_factor_code_invalid;
		}
		return errors;
	},

	validateGroup: function(group){
		var error_msg = locate(Em.I18n.translations, 'errors');

		var errors = {is_not_empty: false};

		["name", "recipients"].forEach(function(property){
			if(Validator.is_empty(group.get(property))){
				errors[property] = error_msg.field_required;
				errors.is_not_empty = true;
			}
		});

		return errors;
	},

	is_empty: function(value) {
		return (value === undefined || String(value).trim().length === 0) ? true : false;
	}
};

AccountValidator = {
	getValidationErrorsFor: function(account) {
		var data = new ValidatorData(locate(Em.I18n.translations, 'errors'), account);

		data.errors = this._info_fields_errors(data);

		data.errors = account.generateSysPassword ?
			data.errors :
			PresenceValidator.getErrors(data, ["password", "passwordConfirm"]);

		data.errors = EqualPasswordsValidator.getErrors(data, "password", "passwordConfirm");

		data.errors = EmailValidator.getErrors(data, account.get("email"));

		data.errors = PhoneNumberValidator.getErrors(data, account.get("mobilePhone"));

		return data.errors;
	},

	getPasswordValidationErrorsFor: function(account){
		var data = new ValidatorData(locate(Em.I18n.translations, 'errors'), account);

		data.errors = PresenceValidator.getErrors(data, ["newPassword"]);

		return data.errors;
	},

	getUpdateValidationErrorsFor: function(account){
		var data = new ValidatorData(locate(Em.I18n.translations, 'errors'), account);

		data.errors = this._info_fields_errors(data);

		data.errors = EmailValidator.getErrors(data, account.get("email"));

		data.errors = PhoneNumberValidator.getErrors(data, account.get("mobilePhone"));

		return data.errors;
	},

	_info_fields_errors: function(data) {
		return PresenceValidator.getErrors(
				data,
				["name", "firstName", "lastName", "email", "mobilePhone"]
			);
	}
};

PasswordValidator = {
	getErrorsFor: function(object, password, confirm){
		var data = new ValidatorData(locate(Em.I18n.translations, 'errors'), object);

		data.errors = EqualPasswordsValidator.getErrors(data, password, confirm);

		data.errors = PresenceValidator.getErrors(data, [password, confirm]);

		return data.errors;
	}
};

ValidatorData = function(error_msg, validationSubject) {
	this.errors = {
		is_not_empty: false
	};

	this.error_msg = error_msg;

	this.validationSubject = validationSubject;
};

PresenceValidator = {
	getErrors: function(data, fields) {
		fields.forEach(function(property){
			if(EmptyValidator.is_empty(data.validationSubject.get(property))){
				data.errors[property] = data.error_msg.field_required;
				data.errors["is_not_empty"] = true;
			}
		});

		return data.errors;
	},
	validate: function(subject, fields) {
		return this.getErrors(
			new ValidatorData(locate(Em.I18n.translations, 'errors'), subject),
			fields
		);
	}
};

EmptyValidator = {
	is_empty: function(value) {
		return (Ember.isNone(value) || String(value).trim().length === 0);
	}
};

EqualPasswordsValidator = {
	getErrors: function(data, passwordField, confirmField) {
		password = data.validationSubject.get(passwordField);
		confirm = data.validationSubject.get(confirmField);

		if (password !== confirm){
			data.errors[passwordField] = data.error_msg.password_mismatch;
			data.errors[confirmField] = data.error_msg.password_mismatch;
			data.errors["is_not_empty"] = true;
		}
		return data.errors;
	}
};

EmailValidator = {
	getErrors: function(data, email) {
		var pattern = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
		if (!pattern.test(email)){
			data.errors["email"] = data.error_msg.email_invalid;
			data.errors["is_not_empty"] = true;
		}
		return data.errors;
	}
};

PhoneNumberValidator = {
	getErrors: function(data, number) {
		var pattern = /^[0-9]([0-9]|[\s]|[-])*$/;
		if (!pattern.test(number)){
			data.errors["mobilePhone"] = data.error_msg.not_a_phone_number;
			data.errors["is_not_empty"] = true;
		}
		return data.errors;
	}
};


Timer = {
	ms_in_s: 1000,
	ms2s: function(ms) {
		return ms/this.ms_in_s;
	},
	s2ms: function(s) {
		return s * this.ms_in_s;
	}
}
