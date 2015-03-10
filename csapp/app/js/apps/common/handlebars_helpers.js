Ember.Handlebars.helper('app-name', function(name) {
	var appName = Em.I18n.translations['appNames'][name];
	return Ember.isEmpty(appName) ? name : appName;
});

Ember.Handlebars.helper('cloud-type-field', function(cloudType, field) {

	var cloudTypeTranslations = Em.I18n.translations['cloudTypes'][cloudType];
	if (!cloudTypeTranslations) return '??undefined translation: cloudTypes.'+cloudType+'.'+field;

	var name = cloudTypeTranslations[field];
	if (!name) return '??undefined translation: cloudTypes.'+cloudType+'.'+field;

	return new Handlebars.SafeString(name);
});

Ember.Handlebars.helper('cloud-option-name', function(cloudType, optionName) {
	var cloudTypeTranslations = Em.I18n.translations['cloudTypes'][cloudType];
	if (!cloudTypeTranslations) return '??undefined translation: cloudTypes.'+cloudType+'.'+field;

	var name = cloudTypeTranslations['options'][optionName];
	if (!name) return '??undefined translation: cloudTypes.'+cloudType+'.options.'+optionName;

	return name;
});

Ember.Handlebars.helper('app-usage', function(usage) {
	var appUsage = Em.I18n.translations['appUsage'][usage];
	if (!appUsage) return '??undefined translation: appUsage.'+usage;
	return appUsage;
});

Ember.Handlebars.helper('task-description', function(result) {
	var action = Em.I18n.translations['task'][result.actionMessageKey];
	if (!action) return '??undefined translation: task.'+result.actionMessageKey;
	if (result.target) action += ": " + result.target;
	return action;
});

Ember.Handlebars.helper('task-event', function(key) {
	var value = Em.I18n.translations['task']['events'][key];
	if (!value) return '??undefined translation: task.events.'+key;
	return value;
});

Ember.Handlebars.helper('fromUTC', function(timeUTC) {
	var convertedDate = new Date(0);
	convertedDate.setUTCSeconds(Math.round(timeUTC/1000));
	return convertedDate.toLocaleString();
});
