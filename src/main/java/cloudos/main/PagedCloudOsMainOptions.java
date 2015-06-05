package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.ResultPage;
import org.kohsuke.args4j.Option;

public class PagedCloudOsMainOptions extends CloudOsMainOptions {

    public static final String USAGE_PAGENUM = "The page number. Default is 1 (first page)";
    public static final String OPT_PAGENUM = "-n";
    public static final String LONGOPT_PAGENUM = "--page-number";
    @Option(name=OPT_PAGENUM, aliases=LONGOPT_PAGENUM, usage=USAGE_PAGENUM)
    @Getter @Setter private int pagenum = 1;

    public static final String USAGE_PAGESIZE = "The page size. Default is 100";
    public static final String OPT_PAGESIZE = "-z";
    public static final String LONGOPT_PAGESIZE = "--page-size";
    @Option(name=OPT_PAGESIZE, aliases=LONGOPT_PAGESIZE, usage=USAGE_PAGESIZE)
    @Getter @Setter private int pagesize = 100;

    public static final String USAGE_QUERY = "The query. Default is empty (search everything)";
    public static final String OPT_QUERY = "-q";
    public static final String LONGOPT_QUERY = "--query";
    @Option(name=OPT_QUERY, aliases=LONGOPT_QUERY, usage=USAGE_QUERY)
    @Getter @Setter private String filter;

    public ResultPage getPage () {
        return new ResultPage()
                .setPageNumber(pagenum)
                .setPageSize(pagesize)
                .setFilter(filter);
    }

}
