App.SettingsRoute = CloudOSProtectedRoute.extend({
	model: function () {
		return {
			"current_password": "",
			"new_password": "",
			"new_password2": ""
		}
	}
});
