package aiai.ai.launchpad;

import aiai.ai.launchpad.experiment.ExperimentService;
import aiai.ai.launchpad.repositories.StationsRepository;
import lombok.Getter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Getter
@Profile("launchpad")
public class RealLaunchpadService implements LaunchpadService{
    private final StationsRepository stationsRepository;
    private final ExperimentService experimentService;

    public RealLaunchpadService(StationsRepository stationsRepository, ExperimentService experimentService) {
        this.stationsRepository = stationsRepository;
        this.experimentService = experimentService;
    }
}
