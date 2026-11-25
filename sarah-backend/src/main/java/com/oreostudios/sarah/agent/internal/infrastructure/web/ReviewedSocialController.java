package com.oreostudios.sarah.agent.internal.infrastructure.web;

import com.oreostudios.sarah.audit.api.AuditFacade;
import com.oreostudios.sarah.campaign.api.*;
import com.oreostudios.sarah.connector.api.*;
import com.oreostudios.sarah.publishing.api.*;
import com.oreostudios.sarah.shared.Platform;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketing/reviewed-social")
@PreAuthorize("hasRole('OWNER')")
public class ReviewedSocialController {
    private final CampaignFacade campaigns; private final ConnectorFacade connectors;
    private final PublishingFacade publishing; private final AuditFacade audit;
    public ReviewedSocialController(CampaignFacade campaigns,ConnectorFacade connectors,PublishingFacade publishing,AuditFacade audit){
        this.campaigns=campaigns;this.connectors=connectors;this.publishing=publishing;this.audit=audit;
    }
    public record Approval(UUID campaignId,UUID draftId,UUID channelId,boolean confirmed){}
    @PostMapping
    public PublicationView approve(@RequestBody Approval a,Authentication auth){
        if(!a.confirmed()||a.campaignId()==null||a.draftId()==null||a.channelId()==null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Explicit owner approval is required");
        if(campaigns.get(a.campaignId()).status().isTerminal())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Campaign has ended");
        DraftView d=campaigns.draftsFor(a.campaignId()).stream().filter(x->x.id().equals(a.draftId())).findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Draft not found"));
        Platform platform=Platform.fromCode(d.platform());
        if(d.status()!=DraftStatus.APPROVED|| (platform!=Platform.REDDIT&&platform!=Platform.INSTAGRAM))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"An approved Instagram or Reddit draft is required");
        ChannelView channel=connectors.listChannels().stream().filter(x->x.id().equals(a.channelId())).findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Channel not found"));
        if(channel.type()!=platform||!channel.live()||channel.status()!=ConnectorStatus.CONNECTED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Select a connected live channel for this platform");
        PublicationView result=publishing.schedule(new SchedulePublicationCommand(a.campaignId(),d.id(),channel.id(),platform,
                d.body(),Instant.now(),1,"reviewed-social:"+d.id()));
        audit.record(auth.getName(),"SOCIAL_OWNER_APPROVED","publication",result.id().toString(),"platform="+platform.code());
        return result;
    }
}
