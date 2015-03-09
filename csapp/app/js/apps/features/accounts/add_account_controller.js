App.AddAccountController = App.BaseAccountController.extend({
	content: {},

	actions: {
		doCreateAccount: function () {

			var account = App.Account.create(this._formData());

			var accountErrors = AccountValidator.getValidationErrorsFor(account);

			if (accountErrors.is_not_empty){
				this._handleAccountValidationErrors(accountErrors);
			}
			else{
				account.save() ?
					this.transitionToAccounts() :
					this._handleAccountUpdateFailed(account);
			}
		},
		cancelCreate: function() {
			if (confirm("Cancel changes ?") == true) {
				this.transitionToAccounts();
			} else {
				// nada
			}
		}
	},

	toggleSysPassword: function(){
		this.set('generateSysPassword', !this.get('generateSysPassword'));
	},

	twoFactor: true,

	generateSysPassword:true,

	primaryGroups:["Admin","User"],

	selectedGroup:"User",

	countryList: Countries.list,

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

	_handleAccountUpdateFailed: function(account) {
		alert('error creating account: ' + account.name)
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
			password: this.get('password'),
			passwordConfirm: this.get('passwordConfirm'),
			generateSysPassword: this.get('generateSysPassword')
		};
	}
});
