
App.AccountsRoute = Ember.Route.extend({
	model: function () {
		return App.Account.findAll();
	}
});
