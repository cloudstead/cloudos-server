App.ManageAccountController = App.BaseAccountController.extend({
	actions: {
		doUpdateAccount: function () {
			var account = this.get('model');

			var accountErrors = AccountValidator.getUpdateValidationErrorsFor(account);

			if (accountErrors.is_not_empty){
				this._handleAccountValidationErrors(accountErrors);
			}
			else{
				this._updateAcount(account);
			}
		},

		doDeleteAccount: function (name) {
			if (Api.delete_account(name)) {
				this.transitionToAccounts();
			}
		},
		cancelCreate: function() {
			if (confirm("Cancel changes ?") == true) {
				this.transitionToAccounts();
			} else {
				// nada
			}
		},
		openChangePassword: function() {
			this.transitionToRoute("manageAccount.adminChangePassword");
		}
	},

	changePassword: false,

	primaryGroups: ["Admin","User"],

	countryList: Countries.sortedList(),

	selectedGroup: function() {
		return this.get('model').admin ? this.primaryGroups[0] : this.primaryGroups[1];
	}.property("selectedGroup", "model"),

	selectedCountry: function() {
		var countryCode = this.get('model').mobilePhoneCountryCode;

		return Countries.findByCode(countryCode);
	}.property("selectedCountryCode", "model"),

	_updateAcount: function(account) {
		account.updateWith(this._formData()) ?
			this.transitionToAccounts() :
			this._handleAccountUpdateFailed(account);
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

	_handleAccountUpdateFailed: function(account) {
		alert('error updating account: ' + account.name)
	},

	_formData: function(){
		return {
			name: this.get('accountName'),
			accountName: this.get('accountName'),
			firstName: this.get('firstName'),
			lastName: this.get('lastName'),
			email: this.get('email'),
			mobilePhone: this.get('mobilePhone'),
			mobilePhoneCountryCode: this.get("selectedCountry")["code"],
			admin: this.get('selectedGroup') == 'Admin' ? true : false,
			twoFactor: this.get('twoFactor'),
			suspended: false,
		};
	}
});
