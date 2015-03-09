App.SingleConfigCategoryController = Ember.ObjectController.extend({
	isAdminCategory: function(){
		// return false
		return this.get("name") === "init";
	}.property("name"),

	isActive: function(){
		return this.get("name");
	}.property("name"),
});
