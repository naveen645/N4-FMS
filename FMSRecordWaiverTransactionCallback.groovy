/*
* Copyright (c) 2019 WeServe LLC. All Rights Reserved.
*
*/
import com.navis.argo.ArgoExtractField
import com.navis.argo.ContextHelper
import com.navis.argo.business.atoms.GuaranteeTypeEnum
import com.navis.argo.business.extract.ChargeableUnitEvent
import com.navis.external.framework.persistence.AbstractExtensionPersistenceCallback
import com.navis.framework.business.Roastery
import com.navis.framework.persistence.HibernateApi
import com.navis.framework.portal.*
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.framework.util.internationalization.UserMessage
import com.navis.framework.util.message.MessageLevel
import com.navis.inventory.InventoryBizMetafield
import com.navis.inventory.business.InventoryFacade
import com.navis.inventory.business.api.UnitField
import com.navis.inventory.business.units.UnitFacilityVisit
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*
 *
 * @Author <a href="mailto:anaveen@servimostech.com">Naveen A</a>, 12/JULY/2019
 *
 * Requirements : This groovy is used to update multiple records for waiver.
 *
 * @Inclusion Location	: Incorporated as a code extension of the type TRANSACTED_BUSINESS_FUNCTION.
 *
 */

class FMSRecordWaiverTransactionCallback extends AbstractExtensionPersistenceCallback {
    @Override
    void execute(Map inputParams, Map inOutResults) {

        LOGGER.setLevel(Level.DEBUG)
        LOGGER.debug("FMSRecordWaiverTransactionCallback started execution!!!!!!!")

        List<Serializable> gkeyList = inputParams.get("GKEYS")
        FieldChanges fieldChanges = (FieldChanges) inputParams.get("FIELD_CHANGES")


        for (int i = 0; i < gkeyList.size(); i++) {
            FieldChanges newFieldChanges = new FieldChanges(fieldChanges);
            Serializable gkey = gkeyList.get(i)
            UnitFacilityVisit unitFacilityVisit = UnitFacilityVisit.hydrate(gkey)
            ChargeableUnitEvent chargeableUnitEvent
            if (unitFacilityVisit != null) {
                String unitId = unitFacilityVisit.getUfvUnit().getUnitId()
                Serializable extractEventType = (Serializable) newFieldChanges.findFieldChange(InventoryBizMetafield.EXTRACT_EVENT_TYPE).getNewValue();
                 chargeableUnitEvent = ChargeableUnitEvent.hydrate(extractEventType)
                if (i!=0) {
                    DomainQuery cueQuery = QueryUtils.createDomainQuery("ChargeableUnitEvent")
                            .addDqPredicate(PredicateFactory.eq(ArgoExtractField.BEXU_UFV_GKEY, gkey))
                            .addDqPredicate(PredicateFactory.eq(ArgoExtractField.BEXU_EVENT_TYPE, chargeableUnitEvent.getEventType()))
                            .addDqPredicate(PredicateFactory.in(ArgoExtractField.BEXU_STATUS, "PARTIAL","QUEUED"))
                    List<ChargeableUnitEvent> chargeableUnitEventList=HibernateApi.getInstance().findEntitiesByDomainQuery(cueQuery)
                    chargeableUnitEvent=chargeableUnitEventList.get(0)
                }

                newFieldChanges.setFieldChange(InventoryBizMetafield.EXTRACT_EVENT_TYPE, chargeableUnitEvent.getPrimaryKey())
                newFieldChanges.setFieldChange(UnitField.UFV_UNIT_ID, unitId)
                newFieldChanges.setFieldChange(ArgoExtractField.GNTE_GUARANTEE_TYPE, GuaranteeTypeEnum.WAIVER);
                newFieldChanges.setFieldChange(ArgoExtractField.GNTE_APPLIED_TO_NATURAL_KEY, unitId);
                newFieldChanges.setFieldChange(ArgoExtractField.GNTE_APPLIED_TO_PRIMARY_KEY, chargeableUnitEvent.getPrimaryKey());

                CrudOperation crud = new CrudOperation(null, 1, "UnitFacilityVisit", newFieldChanges, gkey);
                BizRequest req = new BizRequest(ContextHelper.getThreadUserContext());
                req.addCrudOperation(crud);
                BizResponse response = new BizResponse()
                InventoryFacade inventoryFacade = (InventoryFacade) Roastery.getBean(InventoryFacade.BEAN_ID)
                inventoryFacade.recordWaiverForUfv(req, response)

                if (response.getMessages(MessageLevel.SEVERE)) {
                    List<UserMessage> userMessageList = (List<UserMessage>) response.getMessages(MessageLevel.SEVERE);
                    for(UserMessage message :userMessageList){
                        getMessageCollector().appendMessage(message)
                    }
                }


            }
        }

    }
    private static final Logger LOGGER = Logger.getLogger(FMSRecordWaiverTransactionCallback.class);
}
