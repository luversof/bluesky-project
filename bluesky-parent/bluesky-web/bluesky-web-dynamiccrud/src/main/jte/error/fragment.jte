@import io.github.luversof.boot.exception.BlueskyErrorMessage

@param BlueskyErrorMessage result

!{var message = result.isDisplayableMessage() ? result.getMessage() : result.getErrorCode();}

<div role="alert" class="alert alert-error alert-soft">
	@template._components.icon.alertError()
	<span>${message}</span>
</div>
