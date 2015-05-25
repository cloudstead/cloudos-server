App.AddAccountController = App.BaseAccountController.extend({
	content: {},

	createAnother: false,

	actions: {
		doCreateAccount: function () {

			var account = App.Account.create(this._formData());

			var accountErrors = AccountValidator.getValidationErrorsFor(account);

			if (accountErrors.is_not_empty){
				this._handleAccountValidationErrors(accountErrors);
			}
			else{
				account.save() ?
					this._createAnotherAccount() :
					this._handleAccountUpdateFailed(account);
			}
		},
		cancelCreate: function() {
			if (window.confirm(Em.I18n.translations.sections.acct.cancel_create_account) == true) {
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

	generateSysPassword: true,

	primaryGroups: ["Admin","User"],

	selectedGroup: "User",

	selectedCountry: function() {
		return Countries.findByCode(1);
	}.property("selectedCountryCode", "model"),

	countryList: Countries.sortedList(),

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
	},

	_createAnotherAccount: function() {
		this.set('requestMessages', null);
		this.get("createAnother") ? this._clearForm() : this.transitionToAccounts();
	},

	_clearForm: function() {
		this.set('accountName', "");
		this.set('firstName', "");
		this.set('lastName', "");
		this.set('email', "");
		this.set('mobilePhone', "");
		this.set('twoFactor', true);
		this.set('password', "");
		this.set('passwordConfirm', "");
		this.set('generateSysPassword', true);
		this.set('selectedGroup', "User");
		this.set("selectedCountry", Countries.findByCode(1));
	}
});
