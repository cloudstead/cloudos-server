package cloudos.main;

import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.resources.ApiConstants.CONFIGS_ENDPOINT;

public class SysConfigMain extends CloudOsMainBase<SysConfigMainOptions> {

    public static void main (String[] args) {
        main(SysConfigMain.class, args);
    }

    @Override protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final SysConfigMainOptions options = getOptions();

        if (options.isEmpty()) {
            // nothing given, simply list all apps
            out(api.get(CONFIGS_ENDPOINT).json);

        } else if (options.hasAppName()) {
            if (options.hasCategory() && options.hasConfigOption()) {
                final String uri = CONFIGS_ENDPOINT + "/" + options.getAppName() + "/" + options.getCategory() + "/" + options.getConfigOption();
                if (options.hasValue()) {
                    // app + category + option + value given: update value of option
                    out(api.post(uri, options.getValue()).json);

                } else {
                    // app + category + option given, but no value: list the current value of the single option in the category
                    out(api.get(uri).json);
                }

            } else {
                // category or option is missing, if value is not missing then we're hosed
                if (options.hasValue()) die("Must specify category and option name when updating value");

                if (options.hasCategory() || options.hasConfigOption()) die("Must specify both category and option name");

                // just the app name given, so list all options for the app
                out(api.get(CONFIGS_ENDPOINT+"/"+options.getAppName()).json);
            }

        } else {
            die("No app specified");
        }
    }
}
