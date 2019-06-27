package Generic.FMS

import com.navis.argo.ArgoExtractField
import com.navis.argo.ContextHelper
import com.navis.argo.business.atoms.BillingExtractEntityEnum
import com.navis.argo.business.atoms.GuaranteeTypeEnum
import com.navis.argo.business.atoms.KeySetOwnerEnum
import com.navis.argo.business.reference.ScopedBizUnit
import com.navis.argo.business.security.ArgoUser
import com.navis.argo.business.security.BizGroup
import com.navis.argo.security.ArgoPrivs
import com.navis.control.portal.AbstractExtensionBasedValuesProvider
import com.navis.external.framework.beans.EBean
import com.navis.framework.SecurityField
import com.navis.framework.metafields.IMetafieldDictionary
import com.navis.framework.metafields.MetafieldIdList
import com.navis.framework.persistence.HibernateApi
import com.navis.framework.persistence.hibernate.CarinaPersistenceCallback
import com.navis.framework.persistence.hibernate.PersistenceTemplate
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.UserContext
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.Junction
import com.navis.framework.portal.query.PredicateFactory
import com.navis.framework.portal.query.PredicateIntf
import com.navis.framework.presentation.FrameworkPresentationUtils
import com.navis.framework.presentation.command.VariformUiCommand
import com.navis.framework.presentation.table.ITableDefinition
import com.navis.framework.presentation.view.DefaultSharedUiTableManager
import com.navis.framework.util.BizViolation
import com.navis.framework.util.ValueHolder
import com.navis.framework.util.ValueObject
import com.navis.framework.util.message.MessageCollector
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class customBeanGuaranteeFieldSythesizerUiManager extends DefaultSharedUiTableManager implements EBean {

    private CustomBaseValueProvider _customBaseValueProvider;

    @Override
    protected void init() {
        IMetafieldDictionary mfdDictionary = getRequestContext().getIMetafieldDictionary();
        ITableDefinition tableDefinition = getTableDefinition();
        MetafieldIdList inViewableFields = tableDefinition.getVisibleFields(getRequestContext());
        super.init()
        _customBaseValueProvider = new CustomBaseValueProvider(FrameworkPresentationUtils.getUserContext(), mfdDictionary, inViewableFields)
    }

    @Override
    public List<? extends ValueHolder> createCustomModel(@Nullable Map inArgs) throws BizViolation {
        LOGGER.setLevel(Level.DEBUG)
        return this._customBaseValueProvider != null ? this._customBaseValueProvider.getValueObjects() : new ArrayList();
    }


    class CustomBaseValueProvider extends AbstractExtensionBasedValuesProvider {
        CustomBaseValueProvider(
                @NotNull UserContext inUserContext,
                @NotNull IMetafieldDictionary inMfdDictionary,
                @NotNull MetafieldIdList inMetafieldIdList) {
            super(inUserContext, "FMSGuarnteeFieldSynthesizer", inMfdDictionary, inMetafieldIdList)
        }

        @Override
        List<ValueObject> getValueObjects() {
            LOGGER.setLevel(Level.DEBUG)

            final List<ValueObject> vaoList = new ArrayList();
            PersistenceTemplate pt = new PersistenceTemplate(this._userContext);
            MessageCollector mc = pt.invoke(new CarinaPersistenceCallback() {
                @Override
                protected void doInTransaction() {
                    UserContext context = FrameworkPresentationUtils.getUserContext();
                    DomainQuery domainQuery = (DomainQuery) createQuery()
                    PredicateIntf oacOrCreditPreauthorize = PredicateFactory.in(ArgoExtractField.GNTE_GUARANTEE_TYPE, [GuaranteeTypeEnum.OAC, GuaranteeTypeEnum.CREDIT_PREAUTHORIZE]);
                    PredicateIntf waiver = PredicateFactory.in(ArgoExtractField.GNTE_GUARANTEE_TYPE, [GuaranteeTypeEnum.WAIVER]);
                    PredicateIntf prepay = PredicateFactory.in(ArgoExtractField.GNTE_GUARANTEE_TYPE, [GuaranteeTypeEnum.PRE_PAY]);
                    PredicateIntf paid = PredicateFactory.in(ArgoExtractField.GNTE_GUARANTEE_TYPE, [GuaranteeTypeEnum.PAID]);

                    Junction disjunction = PredicateFactory.disjunction();

                    if ((ArgoPrivs.UNIT_GUARANTEE_VIEW.isAllowed(context)) || (ArgoPrivs.UNIT_GUARANTEE_EDIT.isAllowed(context))) {
                        disjunction.add(oacOrCreditPreauthorize);
                    }
                    if ((ArgoPrivs.UNIT_WAIVER_VIEW.isAllowed(context)) || (ArgoPrivs.UNIT_WAIVER_EDIT.isAllowed(context))) {
                        disjunction.add(waiver);
                    }
                    if ((ArgoPrivs.UNIT_PREPAY_GUARANTEE_VIEW.isAllowed(context)) || (ArgoPrivs.UNIT_PREPAY_GUARANTEE_EDIT.isAllowed(context))) {
                        disjunction.add(prepay);
                    }
                    if ((ArgoPrivs.UNIT_PAID_GUARANTEE_VIEW.isAllowed(context)) || (ArgoPrivs.UNIT_PAID_GUARANTEE_EDIT.isAllowed(context))) {
                        disjunction.add(paid);
                    }

                    if (disjunction.getPredicateCount() > 0) {
                        domainQuery.addDqPredicate(disjunction);
                    }


                    DomainQuery dq = QueryUtils.createDomainQuery("ArgoUser").addDqPredicate(PredicateFactory.eq(SecurityField.BUSER_UID, context.getUserId()));
                    dq.setScopingEnabled(false)
                    List<ArgoUser> userList = HibernateApi.getInstance().findEntitiesByDomainQuery(dq)
                    if (userList != null && userList.size() > 0) {
                        ArgoUser argoUser = (ArgoUser) userList.get(0);
                        if (argoUser != null) {
                            List<Long> lineOprGkey = new ArrayList<>();
                            BizGroup bizGroup = argoUser.getArgouserBizGroup()
                            ScopedBizUnit scopedBizUnit = argoUser.getArgouserCompanyBizUnit()
                            KeySetOwnerEnum keySetOwnerEnum = argoUser.getArgouserMyListChoice()
                            boolean isTerminalUser = false;

                            if ((keySetOwnerEnum != null && KeySetOwnerEnum.USER.equals(keySetOwnerEnum))
                                    && (bizGroup == null && scopedBizUnit == null)) {
                                isTerminalUser = true
                            }
                            if (!isTerminalUser && (keySetOwnerEnum != null && !KeySetOwnerEnum.USER.equals(keySetOwnerEnum)) && (bizGroup != null || scopedBizUnit != null)) {
                                if (KeySetOwnerEnum.BIZGROUP.equals(keySetOwnerEnum) && bizGroup != null) {
                                    HashSet bizuList = argoUser.getAffiliatedBizUnits();
                                    if (bizuList != null) {
                                        for (ScopedBizUnit lineOp : bizuList) {
                                            lineOprGkey.add(lineOp.getBzuGkey())
                                        }
                                    }
                                } else if (keySetOwnerEnum.COMPANY.equals(keySetOwnerEnum) && scopedBizUnit != null) {
                                    LOGGER.info("keysetOwnerEnum is user coming else if flow")
                                    lineOprGkey.add(scopedBizUnit.getBzuGkey())
                                } /*else if (KeySetOwnerEnum.USER.equals(keySetOwnerEnum)) {
                                    if (scopedBizUnit != null) {
                                        lineOprGkey.add(scopedBizUnit.getBzuGkey())
                                    }
                                    if (bizGroup != null) {
                                        lineOprGkey.add(bizGroup.getBizgrpGkey())
                                    }
                                }*/

                                if (lineOprGkey.size() > 0) {
                                    domainQuery.addDqPredicate(PredicateFactory.in(ArgoExtractField.GNTE_GUARANTEE_CUSTOMER, lineOprGkey))
                                } else {

                                    domainQuery.addDqPredicate(PredicateFactory.isNull(ArgoExtractField.GNTE_GKEY))


                                }

                            }

                        }
                    }



                    List<Serializable> gkeys = new ArrayList();
                    Object parent = getAttribute("parent");
                    if (parent != null && parent instanceof VariformUiCommand) {
                        VariformUiCommand parentCommand = (VariformUiCommand) parent;
                        gkeys = (List) parentCommand.getAttribute("source");
                        if (gkeys != null && gkeys.size() > 0) {
                            DomainQuery subQuery1 = QueryUtils.createDomainQuery("ChargeableUnitEvent")
                                    .addDqPredicate(PredicateFactory.eq(ArgoExtractField.BEXU_UFV_GKEY, gkeys.get(0)));
                            domainQuery.addDqPredicate(PredicateFactory.subQueryIn(subQuery1, ArgoExtractField.GNTE_APPLIED_TO_PRIMARY_KEY))
                                    .addDqPredicate(PredicateFactory.eq(ArgoExtractField.GNTE_APPLIED_TO_CLASS, BillingExtractEntityEnum.INV))
                        }
                    }

                    LOGGER.debug("Before fetching the values :: ")
                    vaoList = HibernateApi.getInstance().findValueObjectsByDomainQuery(domainQuery)
                    LOGGER.debug("After fetching the values :: ")
                    // vaoList = HibernateApi.getInstance().findValuesByDomainQuery(domainQuery)
                    /* for (Guarantee guarantee : guaranteeList) {
                         //LOGGER.debug("Guarantee :: $guarantee")
                         *//*LOGGER.debug("Calculated Fields :: " + CustomBaseValueProvider.this._calculatedFields)
                         LOGGER.debug("DB Fields :: " + CustomBaseValueProvider.this._dbFields.addAll(CustomBaseValueProvider.this._calculatedFields))*//*
                         vaoList.add(guarantee.getValueObject(CustomBaseValueProvider.this._dbFields))
                     }*/

                    /*  List<Serializable> gkeys = new ArrayList();
                      Object parent = getAttribute("parent");
                      if (parent != null && parent instanceof VariformUiCommand) {
                          VariformUiCommand parentCommand = (VariformUiCommand) parent;
                          gkeys = (List) parentCommand.getAttribute("source");
                      }*/


                    CustomBaseValueProvider.this.calculateFields(CustomBaseValueProvider.this._dbFields, vaoList);
                }
            })
            return vaoList
        }

        private final static Logger LOGGER = Logger.getLogger(this.class)
    }

    @Override
    String getDetailedDiagnostics() {
        return "customBeanGuaranteeFieldSythesizerUiManager"
    }
    private static final Logger LOGGER = Logger.getLogger(this.class)
}
