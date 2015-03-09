App.AppStoreConfigBasicController = Ember.ArrayController.extend({
	config: {},
	app: {},
	items: [],
	taskId: "",

	installCaption: function() {
		return "Installing " + this.get("app.name");
	}.property("app.name"),

});
