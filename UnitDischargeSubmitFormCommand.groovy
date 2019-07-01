import com.navis.argo.ContextHelper
import com.navis.argo.business.api.ServicesManager
import com.navis.argo.business.atoms.CarrierVisitPhaseEnum
import com.navis.argo.business.reference.Equipment
import com.navis.external.framework.ui.AbstractFormSubmissionCommand
import com.navis.external.framework.util.EFieldChange
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
import com.navis.framework.presentation.ui.command.ISubmitFormCommand
import com.navis.framework.presentation.ui.message.ButtonTypes
import com.navis.framework.presentation.ui.message.MessageDialog
import com.navis.framework.presentation.ui.message.MessageType
import com.navis.framework.presentation.ui.message.OptionDialog
import com.navis.framework.util.BizViolation
import com.navis.framework.util.internationalization.PropertyKeyFactory
import com.navis.framework.util.message.MessageCollector
import com.navis.framework.util.message.MessageLevel
import com.navis.inventory.InventoryBizMetafield
import com.navis.inventory.business.InventoryFacade
import com.navis.inventory.business.api.UnitFinder
import com.navis.inventory.business.units.Unit
import com.navis.inventory.presentation.controller.VesselDischargeFormController
import com.navis.vessel.business.schedule.VesselVisitDetails
import jdk.nashorn.internal.ir.PropertyKey
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*
*
* @Author <a href="mailto:anaveen@servimostech.com">Naveen A</a>, 07/MAY/2019
*
* Requirements : This groovy is used to override the default submitFormCommand for VesselDischarge form.
*
* @Inclusion Location	: Incorporated as a code extension of the type FORM_SUBMISSION_INTERCEPTOR.Copy --> Paste this code (UnitDischargeSubmitFormCommand.groovy)
*
* @Set up in the database backed variform - CUSTOM_INV034- adding action link to call this command and execute it.
*
*/

class UnitDischargeSubmitFormCommand extends AbstractFormSubmissionCommand {



    @Override
    void doBeforeSubmit(String inVariformId, EntityId inEntityId, List<Serializable> inGkeys, EFieldChanges inOutFieldChanges, EFieldChanges inNonDbFieldChanges, Map<String, Object> inParams) {
        // super.doBeforeSubmit(inVariformId, inEntityId, inGkeys, inOutFieldChanges, inNonDbFieldChanges, inParams)
        FieldChanges fieldChange = (FieldChanges) inOutFieldChanges
        long unitDischargeVessel1 =fieldChange!=null ? (long)fieldChange.findFieldChange(InventoryBizMetafield.UNIT_DISCH_VESSEL).getNewValue():null
        LOGGER.info("unitDischargeVessel1::"+unitDischargeVessel1)
        String unitDigits = fieldChange.findFieldChange(InventoryBizMetafield.UNIT_DIGITS).getNewValue()
        String dischargeTimevalue = fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_DATE03)

        PersistenceTemplate persistenceTemplate = new PersistenceTemplate(FrameworkPresentationUtils.getUserContext())
        persistenceTemplate.invoke(new CarinaPersistenceCallback() {
            @Override
            protected void doInTransaction() {
                VesselVisitDetails vesselVisitDetails=VesselVisitDetails.hydrate(unitDischargeVessel1)
                LOGGER.info("vesselVisitDetails value::"+vesselVisitDetails)
                CarrierVisitPhaseEnum carrierVisitPhaseEnum=vesselVisitDetails.getVvdVisitPhase()
                LOGGER.info("carrierVisitPhaseEnum value::"+carrierVisitPhaseEnum)
                String dischargeTime

                UnitFinder finder=(UnitFinder) Roastery.getBean(UnitFinder.BEAN_ID)
                Equipment equipment=Equipment.findEquipment(unitDigits)
                Unit unit=finder.findActiveUnit(ContextHelper.getThreadComplex(),equipment)
                CarrierVisitPhaseEnum carrierVisitPhaseEnum1=unit.getInboundCv().getCvVisitPhase()

                if((CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum)|| CarrierVisitPhaseEnum.CLOSED.equals(carrierVisitPhaseEnum))&&(CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum1)|| CarrierVisitPhaseEnum.CLOSED.equals(carrierVisitPhaseEnum1))){
                    LOGGER.info("carrierVisitPhaseEnum value is wotking coming inside if flow::")
                    if (dischargeTimevalue != null) {
                        dischargeTime = fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_DATE03).getNewValue()

                    } else {
                        dischargeTime = ""
                        OptionDialog.showMessage("Field missing: Enter a value for Discharge Time ", "Vessel Discharge", ButtonTypes.OK, MessageType.ERROR_MESSAGE, null)

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
        if (fieldChange.hasFieldChange(InventoryBizMetafield.UNIT_DIGITS) && fieldChange.hasFieldChange(InventoryBizMetafield.UNIT_YARD_SLOT)) {
            String unitDischVessel = fieldChange.findFieldChange(InventoryBizMetafield.UNIT_DISCH_VESSEL).getNewValue()
            String unitDigits = fieldChange.findFieldChange(InventoryBizMetafield.UNIT_DIGITS).getNewValue()
            LOGGER.info("unitDigits"+unitDigits)
            String unitYardSlot = fieldChange.findFieldChange(InventoryBizMetafield.UNIT_YARD_SLOT).getNewValue()
            String unitChassisValue = fieldChange.findFieldChange(InventoryBizMetafield.UNIT_DISCHARGE_CHASSIS)
            String dischargeTimeValue=fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_DATE03)
            String dischargeNotesValue=fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_STRING04)
            String unitChassis
            String dischargeTime
            String dischargeNotes
            if (unitChassisValue == null) {
                unitChassis = ""

            } else {
                unitChassis = fieldChange.findFieldChange(InventoryBizMetafield.UNIT_DISCHARGE_CHASSIS).getNewValue()
            }

0

            if (dischargeTimeValue == null) {
                dischargeTime=""
                OptionDialog.showMessage("Field missing: Enter a value for Discharge Time ", "Vessel Discharge", ButtonTypes.OK, MessageType.ERROR_MESSAGE, null)

            } else {
                dischargeTime = fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_DATE03).getNewValue()
                LOGGER.info("dischargeTime new value" + dischargeTime)
            }

            if(dischargeNotesValue!=null){
                dischargeNotes=fieldChange.findFieldChange(InventoryBizMetafield.UFV_FLEX_STRING04).getNewValue()
            }else{
                dischargeNotes = ""

            }



            Map parmMap = new HashMap()
            Map results = new HashMap()
            parmMap.put("UNIT_DISCH_VESSEL", unitDischVessel)
            parmMap.put("UFV_REF", unitDigits)
            parmMap.put("UNIT_SLOT", unitYardSlot)
            parmMap.put("UNIT_CHASSIS", unitChassis)
            parmMap.put("DISCHARGE_TIME", dischargeTime)
            parmMap.put("DISCHARGE_NOTES",dischargeNotes)
            MessageCollector messageCollector = executeInTransaction("FMSUnitDischargeCallback", parmMap, results)
            if(results.get("DEFAULT_FLOW")) {
                PersistenceTemplate persistenceTemplate = new PersistenceTemplate(FrameworkPresentationUtils.getUserContext())
                String message

                persistenceTemplate.invoke(new CarinaPersistenceCallback() {
                    @Override
                    protected void doInTransaction() {
                        try {
                            CrudOperation crud = new CrudOperation(null, 1, "Unit", fieldChange, null);
                            BizRequest req = new BizRequest(ContextHelper.getThreadUserContext());
                            req.addCrudOperation(crud);
                            BizResponse response = new BizResponse()
                            InventoryFacade inventoryFacade = (InventoryFacade) Roastery.getBean(InventoryFacade.BEAN_ID)
                           inventoryFacade.dischargeFromVessel(req,response)
                            OptionDialog.showMessage("Unit"+unitDigits+" discharged to slot" + unitYardSlot+"from vessel", "Vessel Discharge", ButtonTypes.OK, MessageType.INFORMATION_MESSAGE, null)

                        }catch(BizViolation e){

                            message = e.getMessage()
                            String key = message.substring(message.indexOf("key=") + 4, message.indexOf("parms="));
                            String value = message.substring(message.indexOf("parms=") + 7, message.indexOf("]"));
                            String[] valueArray = value.split(":");
                            com.navis.framework.util.internationalization.PropertyKey propertyKeyFactory = PropertyKeyFactory.valueOf(key.trim())
                            BizViolation bizViolation = new BizViolation(propertyKeyFactory, null, null, null, valueArray)
                            MessageDialog.showMessageDialog(bizViolation, propertyKeyFactory, AllOtherFrameworkPropertyKeys.VALIDATION__ERROR)
                        }


                    }
                })

            } else if (results.get("RESULT") != null && "SUCCESS".equals(results.get("RESULT"))){
                OptionDialog.showMessage("Unit"+" "+unitDigits+" "+"discharged to slot" + " "+unitYardSlot+" "+"from vessel", "Vessel Discharge", ButtonTypes.OK, MessageType.INFORMATION_MESSAGE, null)

            }
        }
    }


    private static final Logger LOGGER = Logger.getLogger(UnitDischargeSubmitFormCommand.class)
}
