App.AliasRoute = Ember.Route.extend({
	model: function (params) {
		var alias = Api.find_email_alias(params['alias_name']);
		var recipients = [];
		for (var i=0; i<alias.members.length; i++) {
			recipients.push(alias.members[i].name);
		}
		return {
			'aliasName': params['alias_name'],
			'recipients': recipients
		}
	}
});
