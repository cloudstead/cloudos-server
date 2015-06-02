App = Ember.Application.create({
	// for debugging, disable in prod
	LOG_TRANSITIONS: true,
	LOG_ACTIVE_GENERATION: true
});

App.Router.map(function() {
	this.resource('restore');
	this.resource('keys')
});

App.ApplicationController = Ember.ObjectController.extend({});

App.IndexRoute = Ember.Route.extend({
});

App.RequestMessagesObject = Ember.Object.extend({
	_doInitialization: function() {
		var self = this;
		self.set("error", this.get('json')["errors"]);
	}.on('init')
});

App.IndexController = Ember.ObjectController.extend({

	setup_key: function() {
		try{
			return getParameterByName('key');
		}catch(err){
			//
		}
	}.property(),
	name: '',
	initialPassword: '',
	password: '',
	password2: '',
	content: {},
	mobilePhoneCountryCode:'',
	mobilePhone:'',
	email:'',
	firstName:'',
	lastName:'',
	setup_response: null,
	timeZones: function() {
		var time = new Date();
		var tzoffset = time.getTimezoneOffset();
		for (var i = 0; i < timeZoneList.length; i++) {
			if (tzoffset*-1 == timeZoneList[i].ioffset) {
				timeZoneList[i].dfault = true;
				break;
			}
		}
		return timeZoneList;
	}.property(),
	actions: {
		doSetup: function () {
			var setupKey = this.get('setup_key');
			var name = this.get('name');
			var initial_password = this.get('initialPassword');
			var password = this.get('password');
			var password2 = this.get('password2');
			var error_msg = locate(Em.I18n.translations, 'errors');
			var tzone = $('#setupTZfield').find(":selected")[0].index;
			var mobilePhoneCountryCode = this.get('mobilePhoneCountryCode');
			var mobilePhone = this.get('mobilePhone');
			var email = this.get('email');
			var firstName = this.get('firstName');
			var lastName = this.get('lastName');

			var validate = this.validateSetup(
				name, initial_password, password, password2, mobilePhoneCountryCode, mobilePhone, email, firstName, lastName);
			if ( (validate.account_name) || (validate.cs_password) || (validate.password) || (validate.confirm_password)
					|| validate.mobilePhoneCountryCode || validate.mobilePhone || validate.email || validate.firstName || validate.lastName){

				this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null,
								"errors":
									{"name": validate.account_name,
									"initialPassword": validate.cs_password,
									"password": validate.password,
									"password2": validate.confirm_password,
									"mobilePhoneCountryCode": validate.mobilePhoneCountryCode,
									"mobilePhone": validate.mobilePhone,
									"email": validate.email,
									"firstName": validate.firstName,
									"lastName": validate.lastName}}
					  })
					);
				return false;
			}

			var auth_response = Api.setup(setupKey, name, initial_password, password, tzone, mobilePhoneCountryCode, mobilePhone, email, firstName, lastName);

			if (Ember.isNone(auth_response.statusCode)) {
				CloudOs.login(auth_response);
				this.set("setup_response", { restoreKey: auth_response.restoreKey });
				this.transitionToRoute('keys');
			} else {
				this.handleError(auth_response, error_msg);
			}
		}
	},

	validateSetup: function(account_name, cs_password, password, confirm_password, mobilePhoneCountryCode, mobilePhone, email, firstName, lastName) {
		var response = {
			"account_name":null,
			"cs_password":null,
			"password":null,
			"confirm_password":null,
			"mobilePhoneCountryCode":null,
			"mobilePhone":null,
			"email":null,
			"firstName":null,
			"lastName":null
		};

		var error_msg = locate(Em.I18n.translations, 'errors');
		var pattern = /^[a-z][a-z0-9]+$/i;

		if ((account_name.trim() == '') || (!account_name)){
			response.account_name = error_msg.field_required;
		}else if(!pattern.test(account_name)){
			response.account_name = error_msg.account_name_invalid;
		}
		if ((cs_password.trim() == '') || (!cs_password)){
			response.cs_password = error_msg.field_required;
		}
		if ((password.trim() == '') || (!password)){
			response.password = error_msg.field_required;
		}else if(password.length < 8) {
			response.password = error_msg.password_short;
		}
		if ((confirm_password.trim() == '') || (!confirm_password)){
			response.confirm_password = error_msg.field_required;
		}
		if (password != confirm_password) {
			response.confirm_password = error_msg.password_mismatch;
		}
		if ((mobilePhoneCountryCode.trim() == '') || (!mobilePhoneCountryCode)){
			response.mobilePhoneCountryCode = error_msg.field_required;
		}
		if ((mobilePhone.trim() == '') || (!mobilePhone)){
			response.mobilePhone = error_msg.field_required;
		}
		if ((email.trim() == '') || (!email)){
			response.email = error_msg.field_required;
		}
		if ((firstName.trim() == '') || (!firstName)){
			response.firstName = error_msg.field_required;
		}
		if ((lastName.trim() == '') || (!lastName)){
			response.lastName = error_msg.field_required;
		}

		return response;
	},

	// IN CASE THAT ERROR IS CLOUDSTED PASSWORD, SHOW ERROR NEXT TO PASSWORD FIELD, OTHERWISE SHOW ERROR WITH NOTIFY
	handleError: function(auth_response, error_msg){
		var errorMessage = "";
		if(auth_response.errorMessage === undefined || Ember.isEmpty(auth_response.errorMessage)){
			errorMessage = 'Error, perhaps the key was not correct. check your email again.';
		}
		else{
			if(auth_response.errorMessage.indexOf("setup.initialPassword.invalid") !=-1){
				this.set('requestMessages',
					App.RequestMessagesObject.create({
						json: {
							"status": 'error',
							"api_token" : null,
							"errors": {
								"name": null,
								"initialPassword": error_msg[auth_response.errorMessage],
								"password": null,
								"password2": null,
								"mobilePhoneCountryCode": null,
								"mobilePhone": null,
								"email": null,
								"firstName": null,
								"lastName": null
							}
						}
					})
				);
				return false;
			}
			else{
				errorMessage = error_msg[auth_response.errorMessage];
			}
		}
		errorMessage = errorMessage === undefined || Ember.isEmpty(errorMessage) ? "Internal server error" : errorMessage;
		$.notify(errorMessage, { position: "bottom-right", autoHideDelay: 10000, className: 'error' });
	}
});
