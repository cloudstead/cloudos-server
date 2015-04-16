
App.AccountsRoute = CloudOSProtectedRoute.extend({
	model: function () {
		return App.Account.findAll();
	}
});
