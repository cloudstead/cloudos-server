CloudOsStorage = localStorage;

CloudOs = {
	json_safe_parse: function (j) {
		return j ? JSON.parse(j) : null;
	},

	login: function (auth_response) {
		CloudOsStorage.setItem('cloudos_session', auth_response.sessionId);
		CloudOs.set_account(auth_response.account);
	},

	logout: function () {
		CloudOsStorage.clear();
	},

	account: function () {
		var cs_acct = CloudOs.json_safe_parse(CloudOsStorage.getItem('cloudos_account'));
		if (!Ember.isNone(cs_acct)){
			cs_acct.availableApps = Ember.isNone(cs_acct.availableApps) ? [] :
				cs_acct.availableApps.filter(function(app) {
					return app.interactive === true;
				});
			// cs_acct = add_icon_data(cs_acct);

			var excludeApps = ['roundcube', 'roundcube-calendar', 'owncloud'];
			var initialApps = {standardApps: [ ], additionalApps: [] };

			initialApps = _.reduce(cs_acct.availableApps, function (acc, app) {
				 app.addSeparator = false;
				 if(excludeApps.indexOf(app.name) == -1){
				 	app.sortingKey = Ember.isNone(app.assets.taskbarIconAltText) ? app.name : app.assets.taskbarIconAltText
				 	acc.additionalApps.push(app);
				 }
				 else{
				 	acc.standardApps[excludeApps.indexOf(app.name)] = app;
				 }
				return acc;
			}, initialApps);

			var preferredApps = _.compact(initialApps.standardApps);
			
			//add diveder after last prefferd app
			if(preferredApps.length > 0){
				preferredApps[preferredApps.length -1].addSeparator = true;
			}

			cs_acct.availableApps = preferredApps.concat(_.sortBy(initialApps.additionalApps, 'sortingKey'));

			console.log(cs_acct.availableApps, 'cs_acct.availableApps');

		}

		return cs_acct;
	},

	set_account: function (account) {
		CloudOsStorage.setItem('cloudos_account', JSON.stringify(account));
	},

	is_account_valid: function(account) {
		return !account || !account.admin;
	}

	// get_app: function(app_name) {
	// 	var cs_acct = CloudOs.json_safe_parse(CloudOsStorage.getItem('cloudos_account'));
	// 	return cs_acct.availableApps.findBy('name', app_name);
	// }

};

function get_username () {
	const account = CloudOs.account();
	return account ? account.name : null;
}


NotificationStack = {

	getStack: function() {
		var stack = Ember.isEmpty(CloudOsStorage.getItem('notifications')) ? [] : JSON.parse(CloudOsStorage.getItem('notifications'));
		return Ember.isNone(stack) ? [] : stack;
	},

	push: function (messageKey) {
		var stack = this.getStack();
		stack.push(messageKey);
		this.refresh(stack);
	},

	pop: function () {
		var stack = this.getStack();
		var msg = stack.pop();
		this.refresh(stack);
		return msg;
	},

	refresh: function(stack) {
		CloudOsStorage.setItem('notifications', JSON.stringify(stack));
	}
};
