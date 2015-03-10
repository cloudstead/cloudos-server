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
