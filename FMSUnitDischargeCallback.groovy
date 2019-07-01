import com.navis.argo.ContextHelper
import com.navis.argo.business.api.IFlagType
import com.navis.argo.business.api.IImpediment
import com.navis.argo.business.api.ServicesManager
import com.navis.argo.business.atoms.CarrierVisitPhaseEnum
import com.navis.argo.business.atoms.EventEnum
import com.navis.argo.business.atoms.FlagPurposeEnum
import com.navis.argo.business.atoms.LocTypeEnum
import com.navis.argo.business.atoms.LogicalEntityEnum
import com.navis.argo.business.atoms.UnitCategoryEnum
import com.navis.argo.business.atoms.WiMoveKindEnum
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex
import com.navis.argo.business.model.Facility
import com.navis.argo.business.model.LocPosition
import com.navis.argo.business.reference.Equipment
import com.navis.external.framework.persistence.AbstractExtensionPersistenceCallback
import com.navis.external.framework.ui.EUIExtensionHelper
import com.navis.framework.business.Roastery
import com.navis.framework.metafields.MetafieldId
import com.navis.framework.metafields.MetafieldIdFactory
import com.navis.framework.persistence.HibernateApi
import com.navis.framework.persistence.hibernate.CarinaPersistenceCallback
import com.navis.framework.persistence.hibernate.PersistenceTemplate
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.framework.presentation.FrameworkPresentationUtils
import com.navis.framework.presentation.context.PresentationContextUtils
import com.navis.framework.presentation.context.RequestContext
import com.navis.framework.presentation.ui.message.ButtonTypes
import com.navis.framework.presentation.ui.message.MessageType
import com.navis.framework.presentation.ui.message.OptionDialog
import com.navis.framework.util.BizFailure
import com.navis.framework.util.BizViolation
import com.navis.inventory.InvField
import com.navis.inventory.business.api.UnitField
import com.navis.inventory.business.api.UnitFinder
import com.navis.inventory.business.api.UnitManager
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.moves.MoveEvent
import com.navis.inventory.business.units.MoveInfoBean
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.ServicesBizMetafield
import com.navis.services.business.api.EventManager
import com.navis.services.business.event.Event
import com.navis.services.business.rules.EventType
import com.navis.services.business.rules.FlagType
import com.navis.services.business.rules.ServiceRule
import com.navis.vessel.business.schedule.VesselVisitDetails
import com.navis.xpscache.business.atoms.EquipBasicLengthEnum
import com.navis.yard.YardBizMetafield
import com.navis.yard.YardEntity
import org.apache.log4j.Level
import org.apache.log4j.Logger

import java.text.SimpleDateFormat

/*
 *
 * @Author <a href="mailto:anaveen@servimostech.com">Naveen A</a>, 07/MAY/2019
 *
 * Requirements : This groovy is used to discharging the unit from departed vessel.
 *
 * @Inclusion Location	: Incorporated as a code extension of the type TRANSACTED_BUSINESS_FUNCTION.
 *
 */

class FMSUnitDischargeCallback extends AbstractExtensionPersistenceCallback {


    void execute(Map inParms, Map inOutResults) {
        LOGGER.setLevel(Level.DEBUG)
        LOGGER.info("UnitDischargeCallback started execution!!!!!!!!!")

        String dischargeVesselValue = inParms.get("UNIT_DISCH_VESSEL")

        String unitValue = inParms.get("UFV_REF")

        String unitSlotValue = inParms.get("UNIT_SLOT")
        char[] ch=unitSlotValue.toCharArray()
        int length=ch.length

        String unitChassis = inParms.get("UNIT_CHASSIS")
        String unitDischargeTime = inParms.get("DISCHARGE_TIME")
        Date currentDate
        if(unitDischargeTime!=null && !unitDischargeTime.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
            currentDate = sdf.parse(unitDischargeTime);
        }
        String unitDischargeNotes=inParms.get("DISCHARGE_NOTES")

        VesselVisitDetails vesselVisitDetails =VesselVisitDetails!=null ? VesselVisitDetails.hydrate((Serializable) dischargeVesselValue):null
        CarrierVisitPhaseEnum carrierVisitPhaseEnum = vesselVisitDetails!=null ? vesselVisitDetails.getVvdVisitPhase():null
        String cvId = vesselVisitDetails!=null ? vesselVisitDetails.getCvdCv().getCvId():null


        Equipment equipment =Equipment!=null ? Equipment.findEquipment(unitValue):null
        //Unit unit = getUnitFinder().findActiveUnit(ContextHelper.getThreadComplex(), equipment)
        Unit unit=Unit!=null ? getAllUnits(ContextHelper.getThreadComplex(),equipment,vesselVisitDetails.getCvdCv()):null
        LOGGER.info("unit"+unit)

        if(unit==null){
            OptionDialog.showMessage("Invalid unit for the vesselVisit:" , "Vessel Discharge", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
            inOutResults.put("RESULT", "FAIL")

        }else{
            UnitCategoryEnum unitCategory=unit!=null ? unit.getUnitCategory():null
            UnitFacilityVisit unitFacilityVisit=getUfvs(unit,ContextHelper.getThreadFacility())
            //UnitFacilityVisit unitFacilityVisit=unit!=null ? unit.getUnitActiveUfvNowActive():null
            LOGGER.info("unitFacilityVisit"+unitFacilityVisit)
            UfvTransitStateEnum transitStateEnum=unitFacilityVisit!=null ? unitFacilityVisit.getUfvTransitState():null
            String unitCvId =unitFacilityVisit!=null ? unitFacilityVisit.getUfvActualIbCv().getCvId():null
            CarrierVisitPhaseEnum carrierVisitPhaseEnum1=unit.getInboundCv().getCvVisitPhase()



            if(transitStateEnum!=null && UfvTransitStateEnum.S99_RETIRED.equals(transitStateEnum) || UfvTransitStateEnum.S10_ADVISED.equals(transitStateEnum)){
                OptionDialog.showMessage("No ACTIVE Units found with ID" + " "+unitValue, "Vessel Discharge", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
                inOutResults.put("NO_ACTIVE_UNITS","true")
            }else{
                if(unitCategory!=null && (unitCategory.equals(UnitCategoryEnum.IMPORT)|| unitCategory.equals(UnitCategoryEnum.TRANSSHIP) || unitCategory.equals(UnitCategoryEnum.STORAGE))){
                    if(UfvTransitStateEnum!=null &&UfvTransitStateEnum.S20_INBOUND.equals(transitStateEnum)){

                        if (carrierVisitPhaseEnum!=null &&(CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum) || CarrierVisitPhaseEnum.CLOSED.equals(carrierVisitPhaseEnum))&& (CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum1) || CarrierVisitPhaseEnum.CLOSED.equals(carrierVisitPhaseEnum1))) {
                            CarrierVisit carrierVisit =CarrierVisit!=null ? CarrierVisit.findOrCreateVesselVisit(ContextHelper.getThreadFacility(), cvId):null
                            Date cvATD=carrierVisit.getCvATD()
                            Date cvATA=carrierVisit.getCvATA()
                            if(currentDate.after(cvATA) && currentDate.before(cvATD)){
                                EventType eventType

                                String yardSlot
                                if(length==2){
                                    yardSlot=unitSlotValue.substring(0,2)
                                }
                                else {
                                    yardSlot=unitSlotValue.substring(0,3)
                                }
                                int yardBlock = checkYardBlock(yardSlot)
                                if(yardBlock==0){

                                    OptionDialog.showMessage("Invalid position:" +" "+ yardSlot +" "+ "is not a valid position within Yard.", "Vessel Discharge", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
                                    inOutResults.put("RESULT", "FAIL")
                                }else{
                                    EquipBasicLengthEnum equipBasicLengthEnum = equipment.getEqEquipType().getEqtypBasicLength()
                                    LocPosition locPosition =LocPosition!=null ? LocPosition.createYardPosition(ContextHelper.getThreadYard(), unitSlotValue, null, equipBasicLengthEnum, false):null
                                    LOGGER.info("locPosition"+locPosition)
                                    // UnitManager unitManager = (UnitManager) Roastery.getBean(UnitManager.BEAN_ID)
                                    LocPosition locPositionOfUnit =unitFacilityVisit!=null ? unitFacilityVisit.getUfvLastKnownPosition():null
                                    LocPosition newLocPosition=LocPosition.createLocPosition(locPositionOfUnit.resolveLocation(),unitSlotValue,null)

                                    //if (cvId != null && cvId.equals(unitCvId)) {
                                        MoveEvent moveEvent=MoveEvent.recordMoveEvent(unitFacilityVisit,unitFacilityVisit.getUfvLastKnownPosition(),locPosition,carrierVisit, MoveInfoBean.createDefaultMoveInfoBean(WiMoveKindEnum.VeslDisch,currentDate),EventEnum.UNIT_DISCH)
                                        HibernateApi.getInstance().save(moveEvent)

                                      unitFacilityVisit.setFieldValue(InvField.UFV_TIME_OF_LAST_MOVE,currentDate)
                                    eventType = getEventType("UNIT_DISCH")
                                    LOGGER.info("eventType::"+eventType)
                                    EventManager eventManager = (EventManager) Roastery.getBean(EventManager.BEAN_ID)
                                    Event unitEvent = eventType != null ? eventManager.getMostRecentEventByType(eventType, unit) : null;
                                    LOGGER.info("unitEvent::" + unitEvent)
                                    unitEvent.setFieldValue(ServicesBizMetafield.EVNT_APPLIED_DATE, currentDate)

                                       if(UfvTransitStateEnum.S20_INBOUND.equals(unitFacilityVisit.getUfvTransitState())){
                                            unitFacilityVisit.move(locPosition, null)
                                        }
                                        else if(UfvTransitStateEnum.S70_DEPARTED.equals(unitFacilityVisit.getUfvTransitState())){
                                           // MoveEvent moveEvent=MoveEvent.recordMoveEvent(unitFacilityVisit,unitFacilityVisit.getUfvLastKnownPosition(),locPosition,carrierVisit, MoveInfoBean.createDefaultMoveInfoBean(WiMoveKindEnum.VeslDisch,currentDate), EventEnum.UNIT_DISCH)
                                            unitFacilityVisit.setFieldValue(InvField.UFV_TIME_OUT,null)
                                            unitFacilityVisit.setFieldValue(InvField.UFV_LAST_KNOWN_POSITION,locPosition)
                                      //      unitFacilityVisit.setFieldValue(InvField.UFV_TIME_OF_LAST_MOVE,currentDate)
                                            // unitFacilityVisit.correctPosition(newLocPosition, false);
                                            //  unitManager.recordSlotCorrection(unitFacilityVisit, unitSlotValue, null);
                                            //HibernateApi.getInstance().save(moveEvent)
                                        }
                                        unitFacilityVisit.setFieldValue(InvField.UFV_VISIT_STATE, UnitVisitStateEnum.ACTIVE)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_TRANSIT_STATE, UfvTransitStateEnum.S40_YARD)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_TIME_IN, currentDate)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_TIME_OUT,null)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_TIME_OF_LAST_MOVE,currentDate)
                                       // unitFacilityVisit.move(locPosition, null)
                                  //  unitFacilityVisit.setFieldValue(InvField.UFV_ACTUAL_IB_CV,cvId)
                                    //unitFacilityVisit.setFieldValue(UFV_UNIT_CV,cvId)
                                    unitFacilityVisit.updateActualIbCv(CarrierVisit.findCarrierVisit(ContextHelper.getThreadFacility(),LocTypeEnum.VESSEL,cvId))

                                        unitFacilityVisit.updateLastKnownPosition(locPosition, currentDate)
                                        unitFacilityVisit.setFieldValue(UnitField.UFV_FLEX_DATE03, currentDate)
                                        unitFacilityVisit.setFieldValue(UnitField.UFV_FLEX_STRING04,unitDischargeNotes)


                                                eventType = getEventType("UNIT_IN_VESSEL")
                                        unit.recordEvent(eventType, null, "UnitDischarged", currentDate)
                                        eventType=getEventType("DISCHARGE_MANUALLY_BY_USER")
                                        unit.recordEvent(eventType,null,unitDischargeNotes,currentDate)
                                        HibernateApi.getInstance().save(unitFacilityVisit)
                                        HibernateApi.getInstance().flush()
                                        inOutResults.put("RESULT","SUCCESS")

                                   /* }else{
                                        OptionDialog.showMessage("Unit is not routed via"+" "+cvId+"  "+"(Its intended inbound carrier is"+" "+unitCvId+")","Vessel Discharge", ButtonTypes.OK, MessageType.WARNING_MESSAGE,null)
                                        inOutResults.put("IS_VALID_CARRIER_VISIT","false")
                                    }*/
                                }
                            }else{
                                OptionDialog.showMessage("Vessel ATD is passed away","Vessel Discharge", ButtonTypes.OK, MessageType.WARNING_MESSAGE,null)
                                inOutResults.put("RESULT","FAIL")
                            }



                        }else{
                            inOutResults.put("DEFAULT_FLOW", true);

                        }

                    }else{
                        OptionDialog.showMessage("Cannot discharge unit"+" " + unitValue +" "+ "because unit carrier visit"+" " + unitCvId +" "+ "does not match with Visit Reference"+" "+cvId, "Vessel Discharge", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
                        inOutResults.put("IS_VALID_VISIT", "false")
                    }




                }

            }
        }



    }

    private int checkYardBlock(String yardSlotName) throws BizViolation {
        String slot =yardSlotName + "%"
        LOGGER.info("slot" + slot)
        LOGGER.info("yardSlotName" + yardSlotName)
        String slotValue = slot.substring(0, 3)
        LOGGER.info("slotValue::"+slotValue)


        int count
        PersistenceTemplate persistenceTemplate = new PersistenceTemplate(FrameworkPresentationUtils.getUserContext())
        persistenceTemplate.invoke(new CarinaPersistenceCallback() {
            protected void doInTransaction() {


                DomainQuery dq = QueryUtils.createDomainQuery(YardEntity.ABSTRACT_YARD_BLOCK)
                        .addDqPredicate(PredicateFactory.like(YardBizMetafield.ABN_NAME, slotValue))

                List yardBlockList = HibernateApi.getInstance().findEntitiesByDomainQuery(dq)
                LOGGER.info("yardBlockList::" + yardBlockList)
                count = yardBlockList.size()
                LOGGER.info("count::" + count)

            }
        })

        LOGGER.debug("count:: " + count)
        return count;
    }

    private EventType getEventType(String inEventTypeId) {
        if (inEventTypeId == null) {
            return null;
        }
        EventType eventType = EventType.findEventType(inEventTypeId);
        return eventType;
    }

    protected UnitFinder getUnitFinder() {
        return (UnitFinder) Roastery.getBean("unitFinder");
    }

    Unit getAllUnits(Complex complex,Equipment equipment,CarrierVisit carrierVisit){
        Unit unit=null
        DomainQuery unitDomainQuery=QueryUtils.createDomainQuery("Unit")
                .addDqPredicate(PredicateFactory.eq(UnitField.UNIT_EQUIPMENT,equipment.getEqGkey()))
               .addDqPredicate(PredicateFactory.eq(TRANSIT_STATE, UfvTransitStateEnum.S20_INBOUND))
                .addDqPredicate(PredicateFactory.eq(TRANSIT_STATE, UfvTransitStateEnum.S70_DEPARTED))
                .addDqPredicate(PredicateFactory.eq(UnitField.UNIT_COMPLEX,complex.getCpxGkey()))
                .addDqPredicate(PredicateFactory.eq(INBOUND_CV,carrierVisit.getCvId()))
        LOGGER.info("unitDomainQuery::"+unitDomainQuery)
               List<Unit> unitList=HibernateApi.getInstance().findEntitiesByDomainQuery(unitDomainQuery)
        for(Unit unitValue:unitList){
            if(unitList.size()==1) {
                unit = unitValue
            } else{
               unitDomainQuery.addDqPredicate(PredicateFactory.eq(UnitField.UNIT_VISIT_STATE,UnitVisitStateEnum.ACTIVE))
                unitDomainQuery.addDqPredicate(PredicateFactory.eq(TRANSIT_STATE, UfvTransitStateEnum.S20_INBOUND))
                unit = unitValue
                if(unit!=null && !(unit.getActiveUfvNowActiveInAnyUnit().getUfvTransitState().equals(UfvTransitStateEnum.S20_INBOUND))){
                    return
                }



                break
            }
        }
        LOGGER.info("unit value:"+unit)
return unit

    }


    UnitFacilityVisit getUfvs(Unit unit,Facility facility){
        UnitFacilityVisit unitFacilityVisit=null
        DomainQuery ufv=QueryUtils.createDomainQuery("UnitFacilityVisit")
        .addDqPredicate(PredicateFactory.eq(UFV_UNIT,unit.getUnitId()))
                .addDqPredicate(PredicateFactory.eq(InvField.UFV_FACILITY,facility.getFcyGkey()))
             // .addDqPredicate(PredicateFactory.eq(INBOUND_CV,carrierVisit.getCvId()))
        LOGGER.info("ufv"+ufv)
        List<UnitFacilityVisit> ufvList=HibernateApi.getInstance().findEntitiesByDomainQuery(ufv)
        LOGGER.info("ufvList"+ufvList)
        for(UnitFacilityVisit facilityVisit:ufvList){
            unitFacilityVisit=facilityVisit
        }
        LOGGER.info("unitFacilityVisit value:"+unitFacilityVisit)
          return unitFacilityVisit

    }



    private static MetafieldId UFV_UNIT = MetafieldIdFactory.valueOf("ufvUnit.unitId")
    private static final Logger LOGGER = Logger.getLogger(FMSUnitDischargeCallback.class)
    private static MetafieldId INBOUND_CV = MetafieldIdFactory.valueOf("unitActiveUfv.ufvActualIbCv.cvId")
    private static MetafieldId  TRANSIT_STATE = MetafieldIdFactory.valueOf("unitActiveUfv.ufvTransitState")
    private static MetafieldId UFV_UNIT_CV = MetafieldIdFactory.valueOf("ufvUnit.unitDeclaredIbCv")
}