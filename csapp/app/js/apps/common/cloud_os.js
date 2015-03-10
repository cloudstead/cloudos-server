CloudOs = {
	json_safe_parse: function (j) {
		return j ? JSON.parse(j) : null;
	},

	login: function (auth_response) {
		sessionStorage.setItem('cloudos_session', auth_response.sessionId);
		CloudOs.set_account(auth_response.account);
	},

	logout: function () {
		sessionStorage.clear();
	},

	account: function () {
		var cs_acct = CloudOs.json_safe_parse(sessionStorage.getItem('cloudos_account'));
		if (!Ember.isNone(cs_acct)){
			cs_acct.availableApps = Ember.isNone(cs_acct.availableApps) ? [] :
				cs_acct.availableApps.filter(function(app) {
					return app.interactive === true;
				});
			// cs_acct = add_icon_data(cs_acct);
		}
		return cs_acct;
	},

	set_account: function (account) {
		sessionStorage.setItem('cloudos_account', JSON.stringify(account));
	},

	is_account_valid: function(account) {
		return !account || !account.admin;
	}

	// get_app: function(app_name) {
	// 	var cs_acct = CloudOs.json_safe_parse(sessionStorage.getItem('cloudos_account'));
	// 	return cs_acct.availableApps.findBy('name', app_name);
	// }

};

function get_username () {
	const account = CloudOs.account();
	return account ? account.name : null;
}
