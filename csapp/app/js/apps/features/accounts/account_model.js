App.Account = Ember.Object.extend(Ember.Copyable, {

	isSelected: false,

	isActive: function(){
		return this.get("suspended") ? false : true;
	}.property("suspended"),

	accountDOMId: function(){
		return this.get("accountName").trim().replace(" ", "_");
	}.property("accountName"),

	lastLoginTime: function() {
		return Ember.isNone(this.get("lastLogin")) ?
			Em.I18n.translations.sections.acct.no_login_time :
			timestampToString(this.get("lastLogin"));
	}.property("lastLogin"),

	changeCheckboxSelect: function(){
		// 'this' here refers to the checkbox object.
		// it's context is acctually this object.
		account = this.get("context");
		account.get("isSelected");
	},

	destroy: function() {
		var account_destroyed = Api.delete_account(this.get("accountName"));

		if (account_destroyed){
			App.Account.removeAccount(this);
		}

		return account_destroyed;
	},

	toggleStatus: function(){
		this._toggle_suspend();
		var success = this._commit_status_change();
		if (!success){
			this._toggle_suspend();
		}
		return success;
	},

	save: function() {
		return Api.add_account({
			name: this.get('accountName'),
			accountName: this.get('accountName'),
			firstName: this.get('firstName'),
			lastName: this.get('lastName'),
			email: this.get('email'),
			mobilePhone: this.get('mobilePhone'),
			mobilePhoneCountryCode: this.get("mobilePhoneCountryCode"),
			admin: this.get('admin'),
			twoFactor: this.get('twoFactor'),
			suspended: this.get('suspended'),
			password: this.get('password'),
		});
	},

	updateWith: function(data) {
		return Api.update_account(data);
	},

	changePassword: function() {
		return Api.admin_change_password(this.get("name"), this.get("newPassword"));
	},

	status: function() {
		var stat_name = Em.I18n.translations['sections'].acct.status;
		if (this.get("suspended")){
			return stat_name.suspended;
		}
		if (this.get("admin")) {
			return stat_name.admin;
		}
		return stat_name.active;
	}.property("suspended", "admin"),

	_commit_status_change: function(){
		return Api.update_account(
			{
				name: this.get("name"),
				firstName: this.get("firstName"),
				lastName: this.get("lastName"),
				email: this.get("email"),
				emailVerified: false,
				password: "qwe123",
				mobilePhone: ""+this.get("mobilePhone"),
				admin: this.get("admin"),
				twoFactor: this.get("twoFactor"),
				mobilePhoneCountryCode: ""+this.get("mobilePhoneCountryCode"),
				suspended: this.get("suspended"),
				accountName: this.get("accountName")
			}
		);
	},

	_toggle_suspend: function() {
		this.set('suspended', !this.get("suspended"));
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
		return App.Account.create(this._data());
	},
});

App.Account.reopenClass({
	all: Ember.ArrayProxy.create({content: []}),

	findAll: function() {
		var data = Api.list_accounts();
		App.Account.all.clear();
		data.forEach(function(datum) {
			App.Account.all.pushObject(App.Account.create(datum));
		});
		return App.Account.all;
	},

	findByName: function(account_name){
		return App.Account.create(Api.find_account(account_name));
	},

	bulkToggleStatus: function(accounts){
		var success = true;
		accounts.forEach(function(account){
			success = success && account.toggleStatus();
		});
		return success;
	},

	removeAccount: function(account) {
		App.Account.all.removeObject(account);
	}
});
