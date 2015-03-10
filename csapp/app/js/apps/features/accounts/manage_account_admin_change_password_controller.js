App.ManageAccountAdminChangePasswordController = Ember.ObjectController.extend({
	actions:{
		doCloseModal: function(){
			this._transitionToManageAccounts();
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
			this._transitionToManageAccounts();
		}
		else{
			this._handleChangeAccountPasswordFailed(account);
		}
	},

	_transitionToManageAccounts: function() {
		this.transitionToRoute("manageAccount");
	},

	_handleChangeAccountPasswordFailed: function(account) {
		alert('error changing password: ' + account.name)
	}
});
