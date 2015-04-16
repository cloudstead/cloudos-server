App.ManageAccountRoute = CloudOSProtectedRoute.extend({
	model: function (params) {
		return App.Account.findByName(params.name);
	}
});
