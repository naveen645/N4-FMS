import com.navis.argo.ArgoExtractField
import com.navis.extension.portal.ExtensionBeanUtils
import com.navis.extension.portal.IExtensionTransactionHandler
import com.navis.external.framework.beans.EBean
import com.navis.external.framework.ui.EUIExtensionHelper
import com.navis.framework.extension.FrameworkExtensionTypes
import com.navis.framework.metafields.MetafieldId
import com.navis.framework.metafields.MetafieldIdFactory
import com.navis.framework.portal.UserContext
import com.navis.framework.portal.query.DataQuery
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.framework.presentation.command.VariformUiCommand
import com.navis.framework.presentation.context.PresentationContextUtils
import com.navis.framework.presentation.context.RequestContext
import com.navis.framework.presentation.util.PresentationConstants
import com.navis.framework.util.BizFailure
import com.navis.framework.util.internationalization.PropertyKey
import com.navis.framework.util.internationalization.PropertyKeyFactory
import com.navis.framework.util.message.MessageCollector
import com.navis.framework.util.message.MessageLevel
import com.navis.inventory.presentation.UnitGuaranteeUiTableManager
import com.navis.road.business.util.RoadBizUtil
import org.apache.log4j.Level
import org.apache.log4j.Logger

class customBeanGuaranteeTableManager extends UnitGuaranteeUiTableManager implements EBean {


    public static final Logger logger = Logger.getLogger(customBeanGuaranteeTableManager.class)

    public CustomMyUnitsViewUiTableManager() {
    }


    private VariformUiCommand getBaseParentCommand() {
        Object parent = getAttribute(PresentationConstants.PARENT);
        return parent != null && parent instanceof VariformUiCommand ? (VariformUiCommand) parent : null;
    }


    private String getLoggedInUserId() {
        String userId;
        UserContext userContext = getUserContext();
        if (userContext != null) {
            userId = userContext.getUserId()
        }
        return userId;
    }


    public UserContext getUserContext() {
        if (PresentationContextUtils != null && PresentationContextUtils.getRequestContext()) {
            return PresentationContextUtils.getRequestContext().getUserContext();
        } else {
            return UserContext.getThreadUserContext();
        }
    }


    public DataQuery createQuery() {
        logger.setLevel(Level.DEBUG)
        logger.debug("customBeanGuaranteeTableManager started execution")
        DomainQuery domainQuery = super.createQuery()
        Serializable[] ufvKey = null;
        String userValue = getLoggedInUserId()

        Map parmMap = new HashMap()
        Map result = new HashMap()
        parmMap.put("USER_ID", userValue)

        IExtensionTransactionHandler handler = ExtensionBeanUtils.getExtensionTransactionHandler()
        MessageCollector messageCollector = handler.executeInTransaction(userContext, FrameworkExtensionTypes.TRANSACTED_BUSINESS_FUNCTION, "FMSGuarenteeCallback", parmMap, result)
        logger.info("result::" + result)

        if (result.get("BIZ_GROUP") != null) {
            logger.info(" result coming inside if flow")
            List<Long> bizGroup = (List<Long>) result.get("BIZ_GROUP")

            //DomainQuery dq = QueryUtils.createDomainQuery(ArgoExtractEntity.GUARANTEE);
            if (bizGroup != null) {
                domainQuery.addDqPredicate(PredicateFactory.in(ArgoExtractField.GNTE_GUARANTEE_CUSTOMER, bizGroup))
            }
        }

        if ("FAIL".equals(result.get("RESULT"))) {
            //logger.info("result is fail coming inside" + messageCollector)
            // getExtensionHelper().showMessageDialog(MessageLevel.WARNING, "WARNING","ScopedBizUnit is null")

            //messageCollector.appendMessage(MessageLevel.WARNING, PropertyKeyFactory.valueOf(propertyKey), null, null)
            return domainQuery.addDqPredicate(PredicateFactory.isNull(ArgoExtractField.GNTE_GKEY))

        }
        if ("SUCESS".equals(result.get("RESULT"))) {
            return domainQuery;
        }
        // return domainQuery;

    }


    String getDetailedDiagnostics() {
        return "customBeanCustomMyUnitsViewUiTableManager";
    }


    public final EUIExtensionHelper getExtensionHelper() {
        RequestContext requestContext = this.getRequestContext();
        return (EUIExtensionHelper) requestContext.getBean("UIExtensionHelper");
    }

    private static MetafieldId id = MetafieldIdFactory.valueOf("gnteGuaranteeCustomer.bzuGkey")
    private static PropertyKey propertyKey = (PropertyKey) PropertyKeyFactory.valueOf("GUARANTEE_FORM_ERROR")
}
