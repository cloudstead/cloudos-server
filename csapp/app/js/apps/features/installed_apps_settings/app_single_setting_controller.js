App.AppSingleSettingController = Ember.ObjectController.extend({
	needs: "app_setting",

	app_name: Ember.computed.alias("controllers.app_setting.app_name"),

	hiddenValue: function() {
		return Em.I18n.translations.sections.app_settings.hidden;
	}.property(),

	processedValue: function() {
		var value = this.get('value');
		return value === "__VENDOR__DEFAULT__" ? this.get('hiddenValue') : value;
	}.property("value"),

	shouldShowSave: false,

	valueChanged: function() {
		this.set("shouldShowSave", this.hasValueChanged() && this.isValueAllowed());
	}.observes("processedValue"),

	isValueAllowed: function() {
		return (this.get("processedValue") !== "__VENDOR__DEFAULT__") && (this.get("processedValue") !== this.get('hiddenValue'));
	},

	hasValueChanged: function() {
		return this.get("processedValue") !== this.get("value");
	},

	actions: {
		doSaveValue: function() {
			if (this.isValueAllowed() && this.hasValueChanged()) {
				Api.save_category_config_change(
					this.get("app_name"), this.get("path"), this.get("processedValue"));
			}
		}
	}
});
