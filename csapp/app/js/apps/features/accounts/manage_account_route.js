App.ManageAccountRoute = Ember.Route.extend({
	model: function (params) {
		return App.Account.findByName(params.name);
	}
});
