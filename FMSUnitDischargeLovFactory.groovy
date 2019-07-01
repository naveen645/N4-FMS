/*
* Copyright (c) 2019 WeServe LLC. All Rights Reserved.
*
*/
import com.navis.argo.ArgoField
import com.navis.argo.business.atoms.CarrierVisitPhaseEnum
import com.navis.external.framework.ui.lov.AbstractExtensionLovFactory
import com.navis.external.framework.ui.lov.ELovKey
import com.navis.framework.business.atoms.LifeCycleStateEnum
import com.navis.framework.metafields.MetafieldId
import com.navis.framework.metafields.MetafieldIdFactory
import com.navis.framework.portal.QueryUtils
import com.navis.framework.portal.query.DomainQuery
import com.navis.framework.portal.query.PredicateFactory
import com.navis.framework.presentation.lovs.Lov
import com.navis.framework.presentation.lovs.Style
import com.navis.framework.presentation.lovs.list.DomainQueryLov
import com.navis.vessel.VesselField
import com.navis.vessel.api.VesselVisitField
/*
 *
 * @Author <a href="mailto:anaveen@servimostech.com">Naveen A</a>, 07/MAY/2019
 *
 * Requirements : This groovy is used to fetch all the vessel visit that departed as well
 *
 * except closed,canceled,archived in vessel discharge form
 *
 *
 * @Inclusion Location	: Incorporated as a code extension of the type TRANSACTED_BUSINESS_FUNCTION.
 *
 */
class FMSUnitDischargeLovFactory extends AbstractExtensionLovFactory  {

    public Lov getLov(ELovKey inKey) {
        if (inKey.represents("CustomUnitDischarge")) {
            DomainQuery dq = QueryUtils.createDomainQuery("VesselVisitDetails").
                    addDqPredicate(PredicateFactory.eq(ArgoField.CVD_LIFE_CYCLE_STATE, LifeCycleStateEnum.ACTIVE)).
                    addDqPredicate(PredicateFactory.ne(VesselVisitField.VVD_VISIT_PHASE, CarrierVisitPhaseEnum.CANCELED)).
                    addDqPredicate(PredicateFactory.ne(VesselVisitField.VVD_VISIT_PHASE, CarrierVisitPhaseEnum.ARCHIVED)).
                    addDqField(id);

            return new DomainQueryLov(dq, Style.LABEL_ONLY);
        }
        return null;
    }
    private static MetafieldId id = MetafieldIdFactory.valueOf("cvdCv.cvId")
}

