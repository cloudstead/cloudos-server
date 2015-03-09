
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
