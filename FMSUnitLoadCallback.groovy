/*
 * Copyright (c) 2019 WeServe LLC. All Rights Reserved.
 *
 */


import com.navis.argo.ContextHelper
import com.navis.argo.business.api.IFlagType
import com.navis.argo.business.api.IImpediment
import com.navis.argo.business.api.ServicesManager
import com.navis.argo.business.atoms.*
import com.navis.argo.business.model.CarrierVisit
import com.navis.argo.business.model.Complex
import com.navis.argo.business.model.LocPosition
import com.navis.argo.business.reference.Equipment
import com.navis.external.framework.persistence.AbstractExtensionPersistenceCallback
import com.navis.framework.business.Roastery
import com.navis.framework.metafields.MetafieldId
import com.navis.framework.metafields.MetafieldIdFactory
import com.navis.framework.persistence.HibernateApi
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.framework.presentation.ui.message.ButtonTypes
import com.navis.framework.presentation.ui.message.MessageType
import com.navis.framework.presentation.ui.message.OptionDialog
import com.navis.inventory.InvField
import com.navis.inventory.business.api.UnitField
import com.navis.inventory.business.api.UnitFinder
import com.navis.inventory.business.atoms.UfvTransitStateEnum
import com.navis.inventory.business.atoms.UnitVisitStateEnum
import com.navis.inventory.business.moves.MoveEvent
import com.navis.inventory.business.units.MoveInfoBean
import com.navis.inventory.business.units.Unit
import com.navis.inventory.business.units.UnitFacilityVisit
import com.navis.services.business.api.EventManager
import com.navis.services.business.rules.EventType
import com.navis.services.business.rules.FlagType
import com.navis.services.business.rules.ServiceRule
import com.navis.vessel.business.schedule.VesselVisitDetails
import org.apache.log4j.Level
import org.apache.log4j.Logger

import java.text.SimpleDateFormat

/*
 *
 * @Author <a href="mailto:anaveen@servimostech.com">Naveen A</a>, 07/MAY/2019
 *
 * Requirements : This groovy is called while loading the unit to departed vessel.
 *
 * @Inclusion Location	: Incorporated as a code extension of the type TRANSACTED_BUSINESS_FUNCTION.
 *
 */

class FMSUnitLoadCallback extends AbstractExtensionPersistenceCallback {

    void execute(Map inParms, Map inOutResults) {
        LOGGER.setLevel(Level.DEBUG)
        LOGGER.info("FMSUnitLoadCallback started execution!!!!!!!!!")

        String unitLoadVesselValue=inParms.get("UNIT_LOAD_VESSEL")
        LOGGER.info("unitLoadVesselValue"+unitLoadVesselValue)
        if(unitLoadVesselValue!=null){
            VesselVisitDetails details=VesselVisitDetails.hydrate(unitLoadVesselValue)
            LOGGER.info("details"+details)
            CarrierVisitPhaseEnum carrierVisitPhaseEnum=details.getVvdVisitPhase()
            LOGGER.info("carrierVisitPhaseEnum"+carrierVisitPhaseEnum)
            if(carrierVisitPhaseEnum.WORKING.equals(carrierVisitPhaseEnum)){
                LOGGER.info("working phase coming inside")
                inOutResults.put("Success",true)
            }


        }

        String loadVesselValue = inParms.get("LOAD_VESSEL")
        String unitValue = inParms.get("UFV_ref")

        String unitSlotValue = inParms.get("UNIT_POS")
        String unitLoadedTime = inParms.get("LOADED_TIME")
        LOGGER.info("unitLoadedTime"+unitLoadedTime)
        Date currentDate
        if(unitLoadedTime!=null && !unitLoadedTime.isEmpty()){
            SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
            currentDate = sdf.parse(unitLoadedTime)
        }

        String unitLoadNotes=inParms.get("LOAD_NOTES")
        String chassisAttached = inParms.get("IS_CHASSIS_ATTACHED")

        VesselVisitDetails vesselVisitDetails = loadVesselValue != null ? VesselVisitDetails.hydrate(loadVesselValue) : null;
        CarrierVisitPhaseEnum carrierVisitPhaseEnum = vesselVisitDetails != null ? vesselVisitDetails.getVvdVisitPhase() : null
        String cvId = vesselVisitDetails != null ? vesselVisitDetails.getCvdCv().getCvId() : null

        UnitFinder unitFinder = (UnitFinder) Roastery.getBean(UnitFinder.BEAN_ID)
        Equipment equipment = unitValue != null ? Equipment.findEquipment(unitValue) : null;
        Unit unit = equipment != null ? unitFinder.findActiveUnit(ContextHelper.getThreadComplex(), equipment) : null;

        LOGGER.info("unit::" + unit)
        if (unit == null) {
            OptionDialog.showMessage("No ACTIVE Units found with ID" + unitValue, "Vessel Load", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
            inOutResults.put("NO_ACTIVE_UNITS", "true")
        } else {

            UnitCategoryEnum categoryEnum = unit != null ? unit.getUnitCategory() : null
            LocTypeEnum carrierMode = null;
            if (unit != null && unit.getUnitRouting() != null) {
                carrierMode = unit.getUnitRouting().getRtgDeclaredCv() != null ? unit.getUnitRouting().getRtgDeclaredCv().getCvCarrierMode() : null
            }



            UnitFacilityVisit unitFacilityVisit = unit != null ? unit.getUnitActiveUfvNowActive() : null
            UfvTransitStateEnum transitStateEnum = unitFacilityVisit != null ? unitFacilityVisit.getUfvTransitState() : null
            CarrierVisitPhaseEnum carrierVisitPhaseEnum1=unit.getOutboundCv().getCvVisitPhase()
            LOGGER.info("carrierVisitPhaseEnum1::"+carrierVisitPhaseEnum1)
            CarrierVisit unitActualObCv = unitFacilityVisit != null ? unitFacilityVisit.getUfvActualObCv() : null;
            LOGGER.info("unitActualObCv" + unitActualObCv)
            boolean eventFlag = false
            LocTypeEnum cvCarrierMode=unit.getOutboundCv().getCvCarrierMode()
            UnitCategoryEnum unitCategory = unit != null ? unit.getUnitCategory() : null
            if ((unitCategory.equals(UnitCategoryEnum.EXPORT) || unitCategory.equals(UnitCategoryEnum.STORAGE)) && UfvTransitStateEnum.S40_YARD.equals(transitStateEnum)) {
                if ((((unitActualObCv != null && LocTypeEnum.VESSEL.equals(cvCarrierMode))&& (CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum) || CarrierVisitPhaseEnum.CLOSED.equals(carrierVisitPhaseEnum)))
                        || CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum) || CarrierVisitPhaseEnum.CLOSED.equals(carrierVisitPhaseEnum))&&(CarrierVisitPhaseEnum.DEPARTED.equals(carrierVisitPhaseEnum1)|| carrierVisitPhaseEnum1.CLOSED.equals(carrierVisitPhaseEnum1))) {


                   LOGGER.info("coming inside the mode if flow!!!!!!!!")
                    CarrierVisit carrierVisit = CarrierVisit.findOrCreateVesselVisit(ContextHelper.getThreadFacility(), cvId)
                    String visitcvId = carrierVisit != null ? carrierVisit.getCvId() : null
                    Date cvATD=carrierVisit.getCvATD()
                    Date cvATA=carrierVisit.getCvATA()
                    if(currentDate.after(cvATA) && currentDate.before(cvATD)){
                        String holdEventType
                        EventType eventType
                        FlagType flgType
                        FlagStatusEnum flagStatusEnum
                        ServicesManager servicesManager = (ServicesManager) Roastery.getBean(ServicesManager.BEAN_ID)
                        Collection impedimentsCollection = servicesManager.getImpedimentsForEntity(unit)
                        if (impedimentsCollection != null && impedimentsCollection.size() > 0) {
                            for (IImpediment impediment : impedimentsCollection) {
                                if (impediment != null) {
                                    IFlagType flagType = impediment.getFlagType()
                                    flagStatusEnum = impediment.getStatus()
                                    String flagId = flagType != null ? flagType.getId() : null;
                                    flgType = flagId != null ? FlagType.findFlagType(flagId) : null

                                    Collection serviceRuleForFlagType = ServiceRule.findActiveServiceRuleForFlagType(flgType)

                                    Iterator iterator = serviceRuleForFlagType != null ? serviceRuleForFlagType.iterator() : null

                                    if (iterator != null) {
                                        while (iterator.hasNext()) {
                                            ServiceRule serviceRule = (ServiceRule) iterator.next()

                                            eventType = serviceRule != null ? serviceRule.getSrvrulServiceType() : null;

                                            if (eventType != null) {
                                                if ("UNIT_LOAD".equals(eventType.getEvnttypeId()) && "ACTIVE".equals(flagStatusEnum)) {
                                                    OptionDialog.showMessage("Event type" + holdEventType + " " + "can not be applied to unit:" + unitValue + " " + "Hold" + " " + flagType1 + " " + "exists on unit:" + unitValue, "Vessel Load", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
                                                    inOutResults.put("IS_HOLD_APPLIED", "false")
                                                    eventFlag = true
                                                    break
                                                }
                                            }
                                        }
                                    }


                                }
                            }

                        }


                        if (!eventFlag) {

                            if (unitSlotValue != null && unitSlotValue.length() == 6 && unitSlotValue.matches("[0-9]*")) {
                                LocPosition locPosition = LocPosition.createVesselPosition(carrierVisit, unitSlotValue, null)
                                LocPosition unitLocPosition = unitFacilityVisit != null ? unitFacilityVisit.getUfvLastKnownPosition() : null
                                String unitCvId = unitFacilityVisit != null ? unitFacilityVisit.getUfvActualObCv().getCvId() : null

                                //if (unitCvId != null && (cvId.equals(unitCvId) || LocTypeEnum.VESSEL.equals(cvCarrierMode))) {
                                    LOGGER.info("unitCvId is equal!!!!!!!!!hence returning")
                                    MoveEvent moveEvent = MoveEvent.recordMoveEvent(unitFacilityVisit, unitLocPosition, locPosition, carrierVisit, MoveInfoBean.createDefaultMoveInfoBean(WiMoveKindEnum.VeslLoad, currentDate), EventEnum.UNIT_LOAD)
                                    HibernateApi.getInstance().save(moveEvent)


                                    if(unitFacilityVisit != null){
                                        if (LocTypeEnum.VESSEL.equals(cvCarrierMode)&& unit.getUnitRouting() != null) {
                                            unit.getUnitRouting().setRtgDeclaredCv(CarrierVisit.findCarrierVisit(ContextHelper.getThreadFacility(), LocTypeEnum.VESSEL, cvId))
                                            unitFacilityVisit.updateObCv(CarrierVisit.findCarrierVisit(ContextHelper.getThreadFacility(), LocTypeEnum.VESSEL, cvId))
                                        }
                                        unit.setFieldValue(InvField.UNIT_VISIT_STATE, UnitVisitStateEnum.DEPARTED)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_VISIT_STATE, UnitVisitStateEnum.DEPARTED)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_TRANSIT_STATE, UfvTransitStateEnum.S70_DEPARTED)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_TIME_OUT, currentDate)
                                        EventManager eventManager = (EventManager) Roastery.getBean(EventManager.BEAN_ID)
                                        eventType = getEventType("UNIT_OUT_VESSEL")
                                        unit.recordEvent(eventType, null, "UnitLoaded", currentDate)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_TIME_OUT, null)
                                        unitFacilityVisit.setFieldValue(InvField.UFV_TIME_OF_LOADING, currentDate)
                                        unitFacilityVisit.updateLastKnownPosition(locPosition, currentDate)
                                        unitFacilityVisit.setFieldValue(UnitField.UFV_FLEX_DATE02, currentDate)
                                        unitFacilityVisit.setFieldValue(UnitField.UFV_FLEX_STRING03,unitLoadNotes)
                                        eventType = getEventType("LOADED_MANUALLY_BY_USER")
                                        unit.recordEvent(eventType, null,unitLoadNotes, currentDate)
                                        HibernateApi.getInstance().save(unitFacilityVisit)
                                        HibernateApi.getInstance().flush()
                                        inOutResults.put("RESULT", "SUCCESS")


                                    }
/*


                                } else {
                                    OptionDialog.showMessage("Unit is not routed via" + " " + cvId + "  " + "(Its intended outbound carrier is" + " " + unitCvId + ")", "Vessel Load", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
                                    inOutResults.put("IS_VALID_CARRIER_VISIT", "false")
                                }*/
                            } else {
                                OptionDialog.showMessage(unitSlotValue + "is not a valid stow position for vessel" + " " + vesselVisitDetails.getVvdVessel().getVesName(), "Vessel Load", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
                                inOutResults.put("RESULT", "FAIL")
                            }
                        }
                    }else{
                        OptionDialog.showMessage("Vessel ATD is passed away","Vessel Load", ButtonTypes.OK, MessageType.WARNING_MESSAGE,null)
                        inOutResults.put("RESULT","FAIL")

                    }


                } else {
                    inOutResults.put("DEFAULT_FLOW", true)
                }
            } else {
                OptionDialog.showMessage(" Unit" + " " + unitValue + " " + "because it is not in a yard position", "Vessel Load", ButtonTypes.OK, MessageType.WARNING_MESSAGE, null)
                inOutResults.put("IS_VALID_STATE", "false")
            }

        }

    }

    private EventType getEventType(String inEventTypeId) {
        if (inEventTypeId == null) {
            return null;
        }
        EventType eventType = EventType.findEventType(inEventTypeId);
        return eventType;
    }

   /* Unit getAllUnits(Complex complex, Equipment equipment, CarrierVisit carrierVisit){
        Unit unit=null
        DomainQuery unitDomainQuery=QueryUtils.createDomainQuery("Unit")
                .addDqPredicate(PredicateFactory.eq(UnitField.UNIT_EQUIPMENT,equipment.getEqGkey()))
        //.addDqPredicate(PredicateFactory.eq(TRANSIT_STATE, UfvTransitStateEnum.S20_INBOUND))
        //.addDqPredicate(PredicateFactory.eq(TRANSIT_STATE, UfvTransitStateEnum.S70_DEPARTED))
                .addDqPredicate(PredicateFactory.eq(UnitField.UNIT_COMPLEX,complex.getCpxGkey()))
                .addDqPredicate(PredicateFactory.eq(INBOUND_CV,carrierVisit.getCvId()))
        LOGGER.info("unitDomainQuery::"+unitDomainQuery)
        List<Unit> unitList=HibernateApi.getInstance().findEntitiesByDomainQuery(unitDomainQuery)
        for(Unit unitValue:unitList){ //1,2,3
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

    }*/

    private static final Logger LOGGER = Logger.getLogger(FMSUnitLoadCallback.class)
    private static MetafieldId INBOUND_CV = MetafieldIdFactory.valueOf("unitActiveUfv.ufvActualIbCv.cvId")
}
