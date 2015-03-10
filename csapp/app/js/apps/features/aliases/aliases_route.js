App.AliasesRoute = Ember.Route.extend({
	model: function () {
		return Api.find_email_aliases();
	}
});
