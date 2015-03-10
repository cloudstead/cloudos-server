
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
