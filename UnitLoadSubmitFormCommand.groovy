/*
* Copyright (c) 2019 WeServe LLC. All Rights Reserved.
*
*/


import com.navis.argo.ContextHelper
import com.navis.argo.business.atoms.CarrierVisitPhaseEnum
import com.navis.argo.business.reference.Equipment
import com.navis.external.framework.ui.AbstractFormSubmissionCommand
import com.navis.external.framework.util.EFieldChanges
import com.navis.framework.AllOtherFrameworkPropertyKeys
import com.navis.framework.business.Roastery
import com.navis.framework.metafields.entity.EntityId
import com.navis.framework.persistence.hibernate.CarinaPersistenceCallback
import com.navis.framework.persistence.hibernate.PersistenceTemplate
import com.navis.framework.portal.BizRequest
import com.navis.framework.portal.BizResponse
import com.navis.framework.portal.CrudOperation
import com.navis.framework.portal.FieldChanges
import com.navis.framework.presentation.FrameworkPresentationUtils
import com.navis.framework.presentation.ui.message.ButtonTypes
import com.navis.framework.presentation.ui.message.MessageDialog
import com.navis.framework.presentation.ui.message.MessageType
import com.navis.framework.presentation.ui.message.OptionDialog
import com.navis.framework.util.BizViolation
import com.navis.framework.util.internationalization.PropertyKey
import com.navis.framework.util.internationalization.PropertyKeyFactory
import com.navis.framework.util.message.MessageCollector
import com.navis.inventory.InventoryBizMetafield
import com.navis.inventory.business.InventoryFacade
import com.navis.inventory.business.api.InventoryCargoUtils
import com.navis.inventory.business.api.UnitFinder
import com.navis.inventory.business.units.Unit
import com.navis.vessel.business.schedule.VesselVisitDetails
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*
*
* @Author <a href="mailto:anaveen@servimostech.com">Naveen A</a>, 07/MAY/2019
*
* Requirements : This groovy is used to override the default submitFormCommand for VesselLoad.
*
*
* @Inclusion Location	: Incorporated as a code extension of the type FORM_SUBMISSION_INTERCEPTOR.Copy --> Paste this code (UnitLoadSubmitFormCommand.groovy)
*
* @Set up in the database backed variform - CUSTOM_INV033- adding action link to call this command and execute it.
*
*/

class UnitLoadSubmitFormCommand extends AbstractFormSubmissionCommand {


    @Override
    void doBeforeSubmit(String inVariformId, EntityId inEntityId, List<Serializable> inGkeys, EFieldChanges inOutFieldChanges, EFieldChanges inNonDbFieldChanges, Map<String, Object> inParams) {
       // super.doBeforeSubmit(inVariformId, inEntityId, inGkeys, inOutFieldChanges, inNonDbFieldChanges, inParams)
        FieldChanges fieldChange = (FieldChanges) inOutFieldChanges
        long unitLoadVessel1 =fieldChange!=null ? (long)fieldChange.findFieldChange(InventoryBizMetafield.UNIT_LOAD_VESSEL).getNewValue():null
        LOGGER.info("unitLoadVessel1::"+unitLoadVessel1)
        String unitDigits =fieldChange!=null ? fieldChange.findFieldChange(InventoryBizMetafield.UNIT_DIGITS).getNewValue():null
        String loadedTimevalue = fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_DATE02)
        String loadNotesValue=fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_STRING03)

        PersistenceTemplate persistenceTemplate = new PersistenceTemplate(FrameworkPresentationUtils.getUserContext())
        persistenceTemplate.invoke(new CarinaPersistenceCallback() {
            @Override
            protected void doInTransaction() {
                VesselVisitDetails vesselVisitDetails=VesselVisitDetails.hydrate(unitLoadVessel1)
                LOGGER.info("vesselVisitDetails value::"+vesselVisitDetails)
                CarrierVisitPhaseEnum carrierVisitPhaseEnum=vesselVisitDetails.getVvdVisitPhase()
                LOGGER.info("carrierVisitPhaseEnum value::"+carrierVisitPhaseEnum)
                String loadedTime
                String loadNotes
                UnitFinder finder=(UnitFinder) Roastery.getBean(UnitFinder.BEAN_ID)
                Equipment equipment=Equipment.findEquipment(unitDigits)
                Unit unit=finder.findActiveUnit(ContextHelper.getThreadComplex(),equipment)
                CarrierVisitPhaseEnum carrierVisitPhaseEnum1=unit.getOutboundCv().getCvVisitPhase()

                if((CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum)|| CarrierVisitPhaseEnum.CLOSED.equals(carrierVisitPhaseEnum))&&(CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum1)|| CarrierVisitPhaseEnum.CLOSED.equals(carrierVisitPhaseEnum1))){
                    LOGGER.info("carrierVisitPhaseEnum value is wotking coming inside if flow::")
                    if (loadedTimevalue != null) {
                        loadedTime = fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_DATE02).getNewValue()

                    } else {
                        loadedTime = ""
                        OptionDialog.showMessage("Field missing: Enter a value for Loaded Time ", "Vessel Load", ButtonTypes.OK, MessageType.ERROR_MESSAGE, null)

                    }

                }

            }
        })


    }


    @Override

    void submit(String inVariformId, EntityId inEntityId, List<Serializable> inGkeys, EFieldChanges inOutFieldChanges, EFieldChanges inNonDbFieldChanges, Map<String, Object> inParams) {

        LOGGER.setLevel(Level.DEBUG)
        LOGGER.info("UnitDischargeSubmitFormCommand started execution!!!!!!!!!")

        FieldChanges fieldChange = (FieldChanges) inOutFieldChanges

        if (fieldChange.hasFieldChange(InventoryBizMetafield.UNIT_DIGITS) && fieldChange.hasFieldChange(InventoryBizMetafield.UNIT_STOW_POS)) {

            String unitLoadVessel =fieldChange!=null ? fieldChange.findFieldChange(InventoryBizMetafield.UNIT_LOAD_VESSEL).getNewValue():null
            String unitDigits =fieldChange!=null ? fieldChange.findFieldChange(InventoryBizMetafield.UNIT_DIGITS).getNewValue():null
            String unitStowPos =fieldChange!=null ?  fieldChange.findFieldChange(InventoryBizMetafield.UNIT_STOW_POS).getNewValue():null
            String loadedTimevalue = fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_DATE02)
            String loadNotesValue=fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_STRING03)
            Boolean attachChassis = (Boolean) InventoryCargoUtils.getOptionalField(InventoryBizMetafield.UNIT_ATTACH_CHASSIS, fieldChange);
            String isChassisAttached
            String loadedTime
            String loadNotes
            if (attachChassis != null && attachChassis) {
                isChassisAttached = "with chassis attached"
            } else {
                isChassisAttached = ""
            }

            if (loadedTimevalue != null) {
                loadedTime = fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_DATE02).getNewValue()

            } else {
                loadedTime = ""
                //OptionDialog.showMessage("Field missing: Enter a value for Loaded Time ", "Vessel Load", ButtonTypes.OK, MessageType.ERROR_MESSAGE, null)

            }

            if(loadNotesValue!=null){
                loadNotes=fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_STRING03).getNewValue()
            }else{
                loadNotes = ""
                //OptionDialog.showMessage("Field missing: Enter a value for Load Notes ", "Vessel Load", ButtonTypes.OK, MessageType.ERROR_MESSAGE, null)

            }



            Map parmMap = new HashMap()
            Map results = new HashMap()
            parmMap.put("LOAD_VESSEL", unitLoadVessel)
            parmMap.put("UFV_ref", unitDigits)
            parmMap.put("UNIT_POS", unitStowPos)
            parmMap.put("LOADED_TIME", loadedTime)
            parmMap.put("LOAD_NOTES",loadNotes)
            parmMap.put("IS_CHASSIS_ATTACHED", isChassisAttached)


            LOGGER.info("results:"+results)
            MessageCollector messageCollector = executeInTransaction("FMSUnitLoadCallback", parmMap, results)
            if(results.get("DEFAULT_FLOW")){

            }
            if (results.get("DEFAULT_FLOW")) {

             PersistenceTemplate persistenceTemplate = new PersistenceTemplate(FrameworkPresentationUtils.getUserContext())
                String message
                persistenceTemplate.invoke(new CarinaPersistenceCallback() {
                    @Override
                    protected void doInTransaction() {
                        BizResponse response = new BizResponse()
                        try {
                            CrudOperation crud = new CrudOperation(null, 1, "Unit", fieldChange, null);
                            BizRequest req = new BizRequest(ContextHelper.getThreadUserContext());
                            req.addCrudOperation(crud);
                            InventoryFacade inventoryFacade = (InventoryFacade) Roastery.getBean(InventoryFacade.BEAN_ID)
                            inventoryFacade.loadToVessel(req, response)
                            OptionDialog.showMessage("Unit" + unitDigits + " loaded to " + unitStowPos + "to vessel", "Vessel Load", ButtonTypes.OK, MessageType.INFORMATION_MESSAGE, null)

                        } catch (Exception e) {
                            message = e.getMessage()
                            String key = message.substring(message.indexOf("key=") + 4, message.indexOf("parms="));
                            String value = message.substring(message.indexOf("parms=") + 7, message.indexOf("]"));
                            String[] valueArray = value.split(":");
                            PropertyKey propertyKeyFactory = PropertyKeyFactory.valueOf(key.trim())
                            BizViolation bizViolation = new BizViolation(propertyKeyFactory, null, null, null, valueArray)

                            MessageDialog.showMessageDialog(bizViolation, propertyKeyFactory, AllOtherFrameworkPropertyKeys.VALIDATION__ERROR)


                        }
                    }
                })

            } else if (results.get("RESULT") != null && "SUCCESS".equals(results.get("RESULT"))) {
                OptionDialog.showMessage("Unit" + unitDigits + "is loaded to vessel to stow position is" + " " + unitStowPos, "Vessel Load", ButtonTypes.OK, MessageType.INFORMATION_MESSAGE, null)

            }


        }
    }

    private static final Logger LOGGER = Logger.getLogger(UnitLoadSubmitFormCommand.class)
}

