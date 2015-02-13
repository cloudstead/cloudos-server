App = Ember.Application.create({
	LOG_TRANSITIONS: true // for debugging, disable in prod
});


App.ApplicationRoute = Ember.Route.extend({
	model: function() {
		return {
			cloudos_session: sessionStorage.getItem('cloudos_session'),
			cloudos_account: CloudOs.account()
		};
	},
	setupController: function(controller, model) {

		// is HTML5 storage even supported?
		if (typeof(Storage) == "undefined") {
			alert('Your browser is not supported. Please use Firefox, Chrome, Safari 4+, or IE8+');
			return;
		}

		// do we have an API token?
		if (!model.cloudos_session) {
			this.transitionTo('login');
			return;
		}

		// is the token valid?
		var account = Api.account_for_token(model.cloudos_session);
		if (!account) {
			CloudOs.logout();
			this.transitionTo('login');
			return;
		}

		CloudOs.set_account(account);
		pathArray = window.location.href.split( '/' );
		if (((pathArray[3] == '') || (pathArray[3] == '#') || (pathArray[3] == 'index.html')) && (!pathArray[4]))
		{
			this.transitionTo('app', 'roundcube');
		}
		// this.transitionTo('app', 'files');
		// this.transitionTo('app', AppRuntime.app_model('email'));
	},
	actions:{
		openModal: function(modalName, model){
			this.controllerFor(modalName).set('model',model);
			return this.render(modalName, {
				into: 'application',
				outlet: 'modal'
			});
		},
		closeModal: function(){
			return this.disconnectOutlet({
				outlet: 'modal',
				parentView: 'application'
			});
		}
	}
});

App.IndexRoute = App.ApplicationRoute;

App.Router.map(function() {
	this.resource('login');
	this.resource('logout');
	this.resource('settings');
	this.resource('app', { path: '/app/:app_name' });

	this.resource('profile', function(){
		this.route('changePassword', { path: '/change_password' });
	});

	this.resource('resetPassword', { path: '/reset_password/:token' });
});

App.LogoutRoute = Ember.Route.extend({
	setupController: function(controller, model) {
		CloudOs.logout();
		window.location.replace('/index.html');
		return;
	}
});

App.ApplicationController = Ember.ObjectController.extend({
	cloudos_session: sessionStorage.getItem('cloudos_session'),
	cloudos_account: CloudOs.account(),
	actions: {
		'select_app': function (app_name) {
			this.transitionToRoute('app', app_name);
		}
	}
});

App.EappController = Ember.ObjectController.extend({
	hasTaskbarIcon: function(){
		var assets = this.get('assets');
		return !Ember.isNone(assets) &&
			(!Ember.isNone(assets.taskbarIconUrl) || !Ember.isEmpty(assets.taskbarIconUrl));
	}.property()
});

function get_username () {
	const account = CloudOs.account();
	return account ? account.name : null;
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

App.IndexController = Ember.ObjectController.extend({
	cloudos_account: CloudOs.account(),
	username: get_username()
});

App.LoginRoute = Ember.Route.extend({
	beforeModel: function(transition) {
		this._resetLoginControllerMessages();
	},

	_resetLoginControllerMessages: function() {
		var loginController = this.controllerFor('login');
		loginController.set('notificationForgotPassword', null);
		loginController.set('requestMessages', null);
	}
});

App.LoginController = Ember.ObjectController.extend({
	cloudos_account: CloudOs.account(),
	username: get_username(),
	password: '',
	notificationForgotPassword: null,
	requestMessages: null,
	actions: {
		doLogin: function () {
			var creds = {
				'name': this.get('username'),
				'password': this.get('password')
			};
			var error_msg = locate(Em.I18n.translations, 'errors');
			var validate = this.validateLogin(creds.name,creds.password);

			if ( (validate.username) || (validate.password)){
				  this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null,
								"errors":
									{"username": validate.username,
									"password": validate.password}}
						})
					);
				return false;
			}

			// validation ok, check device cookies
			var ckDeviceId = checkCookie("deviceId");
			var ckDeviceName = checkCookie("deviceName");

			if ((!ckDeviceId) || (!ckDeviceName)){
				setCookie("deviceId", generateDeviceId(), 365);
				setCookie("deviceName", getDeviceName(), 365);
			}

			creds['deviceId'] = getCookie("deviceId");
			creds['deviceName'] = getCookie("deviceName");

			var auth_response = Api.login_account(creds);

			if (auth_response && auth_response.account) {
				CloudOs.login(auth_response);
				window.location.replace('/index.html');
			}
			else if (auth_response && auth_response.sessionId){
				this.send('openModal','twoFactorVerification', creds );
			}
			else {
				// temporary, until the api response delivery is updated
				this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null,
								"errors":
									{"username": error_msg.auth_invalid,
									"password": error_msg.auth_invalid}}
						})
					);
			}
		},

		doForgotPassword: function() {
			var token = this.get('model').token;

			var forgetPasswordErrors = PresenceValidator.validate(this, ['username']);

			if (forgetPasswordErrors.is_not_empty){
				this._handleForgetPasswordErrors(forgetPasswordErrors);
			}
			else{
				Api.forgot_password(this.get("username"));
				this.set('notificationForgotPassword',
					Em.I18n.translations.notifications.forgot_password_email_sent);
			}
		}
	},
	validateLogin: function(username, password){

		var response = {"username": null, "password":null};
		var error_msg = locate(Em.I18n.translations, 'errors');

		if ((!username) || (username.trim() == '')){
			response.username = error_msg.field_required;
		}

		if ((!password) || (password.trim() == '')){
			response.password = error_msg.field_required;
		}
		return response;
	},

	_handleForgetPasswordErrors: function(validationErrors) {
		this.set('requestMessages',
			App.RequestMessagesObject.create({
				json: {
					"status": 'error',
					"api_token" : null,
					"errors": validationErrors
				}
			})
		);
	}
});

App.SettingsRoute = Ember.Route.extend({
	model: function () {
		return {
			"current_password": "",
			"new_password": "",
			"new_password2": ""
		}
	}
});

App.SettingsController = Ember.ObjectController.extend({
	actions: {
		changePassword: function () {
			var newPassword = this.get('new_password');
			var confirmPassword = this.get('new_password2');
			var error_msg = locate(Em.I18n.translations, 'errors');

			var validate = this.validateSettings(this.get('current_password'), this.get('new_password'), this.get('new_password2'));
			if ( (validate.current_password) || (validate.new_password) || (validate.confirm_new_password)){
				this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null,
								"errors":
									{"current_password": validate.current_password,
									"new_password": validate.new_password,
									"new_password2": validate.confirm_new_password}}
					  })
					);
				return false;
			}

			if (Api.change_password(CloudOs.account().name, this.get('current_password'), newPassword)) {
				this.transitionTo('index');
			} else {
				this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null,
								"errors":
									{"current_password": error_msg.password_incorrect}}
					  })
					);
			}
		}
	},
	validateSettings: function(curr_password, new_password, confirm_new_password){

		var response = {"current_password":null,
						"new_password": null,
						"confirm_new_password":null};

		var error_msg = locate(Em.I18n.translations, 'errors');

		if ((!curr_password) || (curr_password.trim() == '')){
			response.current_password = error_msg.field_required;
		}

		if ((!new_password) || (new_password.trim() == '')){
			response.new_password = error_msg.field_required;
		}else if(new_password.length < 8) {
			response.new_password = error_msg.password_short;
		}

		if ((!confirm_new_password) || (confirm_new_password.trim() == '')){
			response.confirm_new_password = error_msg.field_required;
		}

		if (new_password != confirm_new_password) {
			response.confirm_new_password = error_msg.password_mismatch;
		}

		return response;
	}
});

App.app_model = function (app_name) {
	var app_url = "/api/app/load/" + app_name;
	return {
			"app_name": app_name,
			"app_url":  app_url + "?" + Api.API_TOKEN + "=" + sessionStorage.getItem('cloudos_session')
		};
};

App.AppRoute = Ember.Route.extend({
	model: function(params) {
		return App.app_model(params.app_name);
	}
});

App.RequestMessagesObject = Ember.Object.extend({
	_doInitialization: function() {
		var self = this;
		self.set("error", this.get('json')["errors"]);
	}.on('init')
});

App.ApplicationView = Ember.View.extend({
	initFoundation: function () {
		Ember.$(document).foundation();
	}.on('didInsertElement')
});

App.TwoFactorVerificationController = Ember.ObjectController.extend({
	actions:{
		close: function() {
			return this.send('closeModal');
		},
		verifyFactor: function(){

			var data = {
				name: this.get('model')["name"],
				secondFactor: this.get('verifyCode'),
				deviceId: this.get('model')["deviceId"],
				deviceName: this.get('model')["deviceName"]
			};

			var validationError = Validator.validateTwoFactorVerificationCode(data.secondFactor);

			if (validationError.verificationCode){
				this._handleVerificationCodeError(validationError);
			}
			else{
				this.send('_validateLoginResponse', Api.login_account(data));
			}
		},
		_validateLoginResponse: function(response) {
			if (response.account !== "undefined" && response.sessionId !== "undefined") {
				CloudOs.login(response);
				window.location.replace('/index.html');
			}else{
				// TODO display error messages, requires errors from API.
			}
		}
	},
	_handleVerificationCodeError: function(validationError) {
	  this.set('requestMessages',
			App.RequestMessagesObject.create({
				json: {
					"status": 'error',
					"api_token" : null,
					"errors": {
						"verifyCode": validationError.verificationCode
					}
				}
			})
		);
	}
});


App.Profile = Ember.Object.extend(Ember.Copyable, {

	updateWith: function(data) {
		return Api.update_account(data);
	},

	changePassword: function() {
		return Api.admin_change_password(this.get("name"), this.get("newPassword"));
	},

	_commit_status_change: function(){
		return Api.update_account(
			{
				name: this.get("name"),
				firstName: this.get("firstName"),
				lastName: this.get("lastName"),
				email: this.get("email"),
				emailVerified: false,
				mobilePhone: ""+this.get("mobilePhone"),
				admin: this.get("admin"),
				twoFactor: this.get("twoFactor"),
				mobilePhoneCountryCode: ""+this.get("mobilePhoneCountryCode"),
				suspended: this.get("suspended"),
				accountName: this.get("accountName")
			}
		);
	},

	_data: function() {
		return {
			name: this.get("name"),
			firstName: this.get("firstName"),
			lastName: this.get("lastName"),
			email: this.get("email"),
			emailVerified: this.get("emailVerified"),
			mobilePhone: ""+this.get("mobilePhone"),
			admin: this.get("admin"),
			twoFactor: this.get("twoFactor"),
			mobilePhoneCountryCode: ""+this.get("mobilePhoneCountryCode"),
			suspended: this.get("suspended"),
			accountName: this.get("accountName"),
			isSelected: this.get("isSelected")
		};
	},

	copy: function() {
		return App.Profile.create(this._data());
	},

	changePassword: function() {
		console.log(this);
		return Api.change_password_2(
			this.get("name"), this.get("oldPassword"), this.get("newPassword"), this.get("uuid"), false);
	},
});

App.Profile.reopenClass({

	findByName: function(account_name){
		return App.Profile.create(Api.find_account(account_name));
	}
});

App.ProfileRoute = Ember.Route.extend({
	model: function() {
		return App.Profile.findByName(CloudOs.account().name);
	},
	setupController: function(controller, model) {
		controller.set('model', model);
		controller.set('original_model', model.copy());
	}
});

App.ProfileController = Ember.ObjectController.extend({
	actions: {
		doReset: function() {
			var account = this.get('original_model');

			this.set('name', account.get("name"));
			this.set('firstName', account.get("firstName"));
			this.set('lastName', account.get("lastName"));
			this.set('email', account.get("email"));
			this.set('mobilePhone', account.get("mobilePhone"));
		},

		doEditProfile: function() {
			var account = this.get('model');

			var accountErrors = AccountValidator.getUpdateValidationErrorsFor(account);

			if (accountErrors.is_not_empty){
				this._handleAccountValidationErrors(accountErrors);
			}
			else{
				this._updateAcount(account);
			}
		},

		openChangePassword: function() {
			this.transitionToRoute("profile.changePassword");
		}
	},

	_handleAccountValidationErrors: function(errors){
		this.set('requestMessages',
			App.RequestMessagesObject.create({
				json: {
					"status": 'error',
					"api_token" : null,
					"errors": errors
				}
			})
		);
	},

	_updateAcount: function(account) {
		account.updateWith(this._formData()) ?
			this.transitionToRoute("profile") :
			this._handleAccountUpdateFailed(account);
	},

	_handleAccountUpdateFailed: function(account) {
		alert('error updating account: ' + account.name)
	},

	_formData: function(){
		var account = this.get('model');
		return {
			name: this.get('name'),
			accountName: this.get('name'),
			firstName: this.get('firstName'),
			lastName: this.get('lastName'),
			email: this.get('email'),
			mobilePhone: this.get('mobilePhone'),

			mobilePhoneCountryCode: account.get("mobilePhoneCountryCode"),
			admin: account.get('admin'),
			twoFactor: account.get('twoFactor'),
			suspended: account.get('suspended'),
		};
	}
});

App.ProfileChangePasswordRoute = Ember.Route.extend({
	model: function () {
		return this.modelFor('profile');
	},
	renderTemplate: function() {
		this.render('change_password_modal', { outlet: 'change_password', controller: this.controller });
	}
});

App.ProfileChangePasswordController = Ember.ObjectController.extend({
	actions:{
		doCloseModal: function(){
			this._transitionToProfile();
		},

		doChangePassword: function () {
			var account = this.get('model');

			var passwordErrors = AccountValidator.getPasswordValidationErrorsFor(account);

			if (passwordErrors.is_not_empty){
				this._handleChangeAccountPasswordErrors(passwordErrors);
			}
			else{
				this._changePassword(account);
			}
		}
	},

	_handleChangeAccountPasswordErrors: function(errors){
		this.set('requestMessages',
			App.RequestMessagesObject.create({
				json: {
					"status": 'error',
					"api_token" : null,
					"errors": errors
				}
			})
		);
	},

	_changePassword: function(account) {
		if (account.changePassword()){
			this._transitionToProfile();
		}
		else{
			this._handleChangeAccountPasswordFailed(account);
		}
	},

	_transitionToProfile: function() {
		this.transitionToRoute("profile");
	},

	_handleChangeAccountPasswordFailed: function(account) {
		alert('error changing password account: ' + account.name)
	}
});

App.ResetPasswordRoute = Ember.Route.extend({
	model: function (params) {
		return { token : params['token'] };
	}
});

App.ResetPasswordController = Ember.ObjectController.extend({
	actions:{
		doResetPassword: function () {
			var token = this.get('model').token;
			var delayInSeconds = 3;

			var passwordErrors =
				PasswordValidator.getErrorsFor(this, "password", "passwordConfirm");

			if (passwordErrors.is_not_empty){
				this._handleChangeAccountPasswordErrors(passwordErrors);
			}
			else{
				Api.reset_password(token, this.get('password'));

				this.set(
					"resetPasswordSuccessful",
					this._delayMessage(delayInSeconds));

				this._delayedTransitionTo("login", delayInSeconds);
			}

		}
	},

	_handleChangeAccountPasswordErrors: function(errors) {
		this.set('requestMessages',
			App.RequestMessagesObject.create({
				json: {
					"status": 'error',
					"api_token" : null,
					"errors": errors
				}
			})
		);
	},

	_delayedTransitionTo: function(routeName, delayInSeconds){
		TIMER_STEP_IN_SECONDS = 1;
		var passedInSeconds = 0;
		var self = this;

		var interval = setInterval(
			function() {
				passedInSeconds += 1;
				self.set(
					"resetPasswordSuccessful",
					self._delayMessage(parseInt(delayInSeconds - passedInSeconds, 10))
				);
				if (passedInSeconds >= delayInSeconds){
					clearInterval(interval);
					self.transitionToRoute(routeName);
				}
			},
			Timer.s2ms(TIMER_STEP_IN_SECONDS)
		);
	},

	_delayMessage: function(delayInSeconds) {
		return Ember.I18n.translations.notifications.reset_password_successful +
			" " + delayInSeconds + "s.";
	},
});
