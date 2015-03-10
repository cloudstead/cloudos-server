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
