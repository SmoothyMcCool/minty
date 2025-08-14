package tom.ewm.service;

import org.springframework.stereotype.Service;

import tom.api.services.ewm.EwmOslcClient;
import tom.api.services.ewm.EwmOslcQueryService;

@Service
public class EwmOslcQueryServiceImpl implements EwmOslcQueryService {

	public EwmOslcQueryServiceImpl() {
	}

	@Override
	public EwmOslcClient createClient(String server, String username, String password) {
		return new EwmOslcClientImpl(server, username, password);
	}

}
