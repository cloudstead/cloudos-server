App.AppSettingRoute = CloudOSProtectedRoute.extend({
	params: {},

	model: function(params){
		this.set("params", params);
		return Api.get_category_config(params.app_name);
	},

	setupController: function(controller, model, params) {
		this._super(controller, model, params);
		controller.set("app_name", this.get("params").app_name);
	}
});
