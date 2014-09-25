package cloudos.model.support;

import cloudos.appstore.model.AppPrice;
import lombok.Getter;
import lombok.Setter;

public class PriceQuote {

    @Getter @Setter private String isoCurrency;
    @Getter @Setter private AppPrice appPrice;
//    @Getter @Setter private CsCloudPrice preferredCloudPrice;
//    @Getter @Setter private Map<CsCloudType, CsCloudPrice> availableCloudPrices;
//    @Getter @Setter private Map<CsCloudType, CsCloudPrice> otherCloudPrices;

}
