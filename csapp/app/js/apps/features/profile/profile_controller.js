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
